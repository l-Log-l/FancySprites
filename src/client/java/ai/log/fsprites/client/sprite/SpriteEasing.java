package ai.log.fsprites.client.sprite;

/**
 * Easing functions for animation interpolation.
 */
public enum SpriteEasing {
    NONE("none") {
        @Override
        public float apply(float t) {
            return t >= 1.0f ? 1.0f : 0.0f;
        }
    },
    LINEAR("linear") {
        @Override
        public float apply(float t) {
            return t;
        }
    },
    EASE_IN("ease_in") {
        @Override
        public float apply(float t) {
            return t * t;
        }
    },
    EASE_OUT("ease_out") {
        @Override
        public float apply(float t) {
            return t * (2 - t);
        }
    },
    EASE_IN_OUT("ease_in_out") {
        @Override
        public float apply(float t) {
            return t < 0.5f ? 2 * t * t : -1 + (4 - 2 * t) * t;
        }
    },
    BOUNCE("bounce") {
        @Override
        public float apply(float t) {
            if (t < 0.5f) {
                return 2 * t * t;
            } else {
                return -1 + (4 - 2 * t) * t;
            }
        }
    },
    ELASTIC("elastic") {
        @Override
        public float apply(float t) {
            return t == 0 || t == 1 ? t : -((float)Math.pow(2, 10 * (t - 1))) * (float)Math.sin((t - 1.075f) * (2 * (float)Math.PI) / 0.3f);
        }
    },
    STEP("step") {
        @Override
        public float apply(float t) {
            return (float)Math.floor(t * 4) / 4;
        }
    },
    SMOOTH("smooth") {
        @Override
        public float apply(float t) {
            return t * t * (3 - 2 * t);
        }
    };

    public final String id;

    SpriteEasing(String id) {
        this.id = id;
    }

    public abstract float apply(float t);

    public static SpriteEasing fromString(String name) {
        if (name == null || name.isEmpty()) {
            return NONE;
        }
        try {
            return SpriteEasing.valueOf(name.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
