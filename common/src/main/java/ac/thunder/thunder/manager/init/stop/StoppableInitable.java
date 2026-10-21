package ac.thunder.thunder.manager.init.stop;

import ac.thunder.thunder.manager.init.Initable;

public interface StoppableInitable extends Initable {
    void stop();
}
