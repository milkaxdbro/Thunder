package ac.thunder.thunder.utils.data;

import ac.thunder.thunder.utils.math.Vector3dm;

public class HitData {
    Vector3dm blockHitLocation;

    public HitData(Vector3dm blockHitLocation) {
        this.blockHitLocation = blockHitLocation;
    }

    public ac.thunder.thunder.utils.math.Vector3dm getBlockHitLocation() {
        return this.blockHitLocation;
    }
}
