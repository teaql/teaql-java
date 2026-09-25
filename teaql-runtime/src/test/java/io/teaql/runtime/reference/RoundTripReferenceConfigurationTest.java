package io.teaql.runtime.reference;

import io.teaql.core.UserContext;
import io.teaql.core.reference.InternalEntityIdentity;
import io.teaql.core.reference.ResolvedRoundTripReference;
import io.teaql.core.reference.RoundTripReference;
import io.teaql.core.reference.RoundTripReferenceProvider;
import org.junit.Assert;
import org.junit.Test;

public class RoundTripReferenceConfigurationTest {
    private final RoundTripReferenceProvider governed = new RoundTripReferenceProvider() {
        @Override
        public RoundTripReference issue(UserContext context, InternalEntityIdentity identity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResolvedRoundTripReference resolve(
                UserContext context,
                RoundTripReference reference,
                String expectedEntityType) {
            throw new UnsupportedOperationException();
        }
    };

    @Test
    public void exactAcknowledgementSelectsRawOnlyForDevelopmentOrTest() {
        Assert.assertSame(
                governed,
                RoundTripReferenceConfiguration.configuredProvider(
                        "development", "true", governed));
        Assert.assertTrue(
                RoundTripReferenceConfiguration.configuredProvider(
                        "development",
                        RoundTripReferenceConfiguration.RAW_ID_ACKNOWLEDGEMENT,
                        governed)
                        instanceof RawRoundTripReferenceProvider);
        Assert.assertTrue(
                RoundTripReferenceConfiguration.configuredProvider(
                        "test",
                        RoundTripReferenceConfiguration.RAW_ID_ACKNOWLEDGEMENT,
                        governed)
                        instanceof RawRoundTripReferenceProvider);
        Assert.assertThrows(
                IllegalStateException.class,
                () -> RoundTripReferenceConfiguration.configuredProvider(
                        "production",
                        RoundTripReferenceConfiguration.RAW_ID_ACKNOWLEDGEMENT,
                        governed));
    }
}
