package io.github.blaublue1303.brassanchor.compat.create;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import io.github.blaublue1303.brassanchor.ModContent;

public final class CreateCompat {
    private CreateCompat() {}

    public static void init() {
        MovementBehaviour.REGISTRY.register(ModContent.BRASS_ANCHOR.get(), new AnchorMovementBehaviour());
    }
}
