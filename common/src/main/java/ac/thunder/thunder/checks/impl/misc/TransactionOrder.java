package ac.thunder.thunder.checks.impl.misc;

import ac.thunder.thunder.checks.Check;
import ac.thunder.thunder.checks.CheckData;
import ac.thunder.thunder.player.ThunderPlayer;

@CheckData(name = "TransactionOrder", stableKey = "thunder.ping.invalid_transaction_order", description = "Транзакции в неверном порядке")
public class TransactionOrder extends Check {
    public TransactionOrder(ThunderPlayer player) {
        super(player);
    }
}
