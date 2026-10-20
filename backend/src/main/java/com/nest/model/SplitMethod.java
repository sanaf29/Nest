package com.nest.model;

/** Stored split method; each constant creates its strategy (factory method). */
public enum SplitMethod {
    EQUAL {
        @Override
        public SplitStrategy strategy() {
            return new EqualSplitStrategy();
        }
    },
    CUSTOM {
        @Override
        public SplitStrategy strategy() {
            return new CustomAmountSplitStrategy();
        }
    },
    PERCENTAGE {
        @Override
        public SplitStrategy strategy() {
            return new PercentageSplitStrategy();
        }
    };

    public abstract SplitStrategy strategy();
}
