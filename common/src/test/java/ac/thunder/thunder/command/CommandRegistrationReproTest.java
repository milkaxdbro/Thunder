package ac.thunder.thunder.command;

import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.exception.AmbiguousNodeException;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.internal.CommandNode;
import org.incendo.cloud.internal.CommandRegistrationHandler;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.fail;

class CommandRegistrationReproTest {

    static final class TestManager extends CommandManager<Object> {
        TestManager() {
            super(ExecutionCoordinator.simpleCoordinator(), CommandRegistrationHandler.nullCommandRegistrationHandler());
        }

        @Override
        public boolean hasPermission(Object sender, String permission) {
            return true;
        }
    }

    private void handler(CommandContext<Object> ctx) {
    }

    @Test
    void thunderStyleRootPlusLiterals() {
        TestManager manager = new TestManager();
        assertDoesNotThrow(() -> {
            manager.command(manager.commandBuilder("thunder", "th").literal("alerts").handler(this::handler));
            manager.command(manager.commandBuilder("thunder", "th").literal("profile").required("t", StringParser.stringParser()).handler(this::handler));
        });
    }

    @Test
    void bareRootWithLiteralChildren() {
        TestManager manager = new TestManager();
        assertDoesNotThrow(() -> {
            manager.command(manager.commandBuilder("spec").handler(this::handler));
            manager.command(manager.commandBuilder("spec").literal("accept").required("player", StringParser.stringParser()).handler(this::handler));
            manager.command(manager.commandBuilder("spec").literal("dismiss").required("player", StringParser.stringParser()).handler(this::handler));
            manager.command(manager.commandBuilder("spec").literal("stop").handler(this::handler));
        });
    }

    @Test
    void twoRootsSharingOneAliasIsRejected() {
        TestManager manager = new TestManager();
        manager.command(manager.commandBuilder("thunder", "th").literal("alerts").handler(this::handler));
        // A second root reusing alias "th" must fail fast instead of silently shadowing commands.
        try {
            manager.command(manager.commandBuilder("grim", "th").literal("history").handler(this::handler));
            fail("expected AmbiguousNodeException for duplicate root alias");
        } catch (AmbiguousNodeException expected) {
            assertDoesNotThrow(() -> describe(expected.parentNode()));
        }
    }

    @Test
    void literalPlusOptionalString() {
        TestManager manager = new TestManager();
        assertDoesNotThrow(() ->
                manager.command(manager.commandBuilder("thunder", "th").literal("stopspectating")
                        .optional("here", StringParser.stringParser()).handler(this::handler)));
    }

    @Test
    void fullHistoryStyleRegistration() {
        TestManager manager = new TestManager();
        try {
            manager.command(manager.commandBuilder("thunder", "th")
                    .literal("history", "hist").literal("repair").literal("check-ids").handler(this::handler));
            for (boolean withPlayer : new boolean[]{false, true}) {
                manager.command(flagged(manager, base(manager, withPlayer)).handler(this::handler));
                manager.command(flagged(manager, base(manager, withPlayer)
                        .literal("page").required("page_number", IntegerParser.integerParser(1))).handler(this::handler));
                manager.command(base(manager, withPlayer).literal("session").handler(this::handler));
                manager.command(flagged(manager, base(manager, withPlayer)
                        .literal("session").required("session", StringParser.stringParser())
                        .flag(manager.flagBuilder("detailed").withAliases("d")
                                .withDescription(Description.of("x")))
                        .flag(manager.flagBuilder("verbose").withAliases("v")
                                .withDescription(Description.of("y")))).handler(this::handler));
                manager.command(flagged(manager, base(manager, withPlayer)
                        .literal("session").required("session", StringParser.stringParser())
                        .literal("page").required("page_number", IntegerParser.integerParser(1))
                        .flag(manager.flagBuilder("detailed").withAliases("d")
                                .withDescription(Description.of("x")))
                        .flag(manager.flagBuilder("verbose").withAliases("v")
                                .withDescription(Description.of("y")))).handler(this::handler));
            }
            manager.command(manager.commandBuilder("thunder", "th")
                    .literal("history", "hist").literal("copy")
                    .required("src", StringParser.stringParser()).required("dst", StringParser.stringParser())
                    .flag(manager.flagBuilder("delete")).handler(this::handler));
            manager.command(manager.commandBuilder("thunder", "th")
                    .literal("history", "hist").literal("migrate")
                    .flag(manager.flagBuilder("delete")).handler(this::handler));
        } catch (AmbiguousNodeException e) {
            StringBuilder sb = new StringBuilder("AMBIGUITY parent=");
            sb.append(describe(e.parentNode()));
            sb.append(" ambiguous=").append(describe(e.ambiguousNode()));
            sb.append(" children=[");
            for (CommandNode<?> child : e.children()) {
                sb.append(describe(child)).append(",");
            }
            fail(sb.append("]").toString());
        }
    }

    private org.incendo.cloud.Command.Builder<Object> base(TestManager manager, boolean withPlayer) {
        org.incendo.cloud.Command.Builder<Object> b = manager.commandBuilder("thunder", "th")
                .literal("history", "hist");
        if (withPlayer) b = b.literal("player");
        return b.required("target", StringParser.stringParser());
    }

    private org.incendo.cloud.Command.Builder<Object> flagged(TestManager manager, org.incendo.cloud.Command.Builder<Object> b) {
        // Stand-in manager just to build flags; flags don't need the real manager.
        return b.flag(manager.flagBuilder("name")).flag(manager.flagBuilder("match")).flag(manager.flagBuilder("grep"));
    }

    private static String describe(CommandNode<?> node) {
        if (node == null) return "<null>";
        try {
            return node.component() == null ? "<root:null-component>" : node.component().name();
        } catch (Exception ex) {
            return "<err>";
        }
    }
}
