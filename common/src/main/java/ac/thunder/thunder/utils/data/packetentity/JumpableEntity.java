package ac.thunder.thunder.utils.data.packetentity;

import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.utils.data.VectorData;

import java.util.Set;

public interface JumpableEntity {

    boolean isJumping();

    void setJumping(boolean jumping);

    float getJumpPower();

    void setJumpPower(float jumpPower);

    boolean canPlayerJump(ThunderPlayer player);

    boolean hasSaddle();

    void executeJump(ThunderPlayer player, Set<VectorData> possibleVectors);

}
