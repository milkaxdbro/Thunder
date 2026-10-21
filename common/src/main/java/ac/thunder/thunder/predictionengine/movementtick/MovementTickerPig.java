package ac.thunder.thunder.predictionengine.movementtick;

import ac.thunder.thunder.player.ThunderPlayer;
import ac.thunder.thunder.predictionengine.predictions.input.Input;
import ac.thunder.thunder.utils.data.packetentity.PacketEntityRideable;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;

public class MovementTickerPig extends MovementTickerRideable {
    public MovementTickerPig(ThunderPlayer player) {
        super(player);
        this.movementInput = Input.createInput(player, 0, 0, 1);
    }

    @Override
    public float getSteeringSpeed() { // Vanilla multiples by 0.225f
        PacketEntityRideable pig = (PacketEntityRideable) player.compensatedEntities.self.getRiding();
        return (float) pig.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.225f;
    }
}
