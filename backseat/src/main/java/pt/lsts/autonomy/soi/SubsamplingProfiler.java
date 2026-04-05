package pt.lsts.autonomy.soi;

import pt.lsts.imc4j.msg.EstimatedState;
import pt.lsts.imc4j.msg.Message;
import pt.lsts.imc4j.msg.ProfileSample;
import pt.lsts.imc4j.msg.VerticalProfile;
import pt.lsts.imc4j.util.WGS84Utilities;

import java.util.ArrayList;

/**
 * A profile generator that subsamples chronological data by selecting equally spaced elements. This implementation
 * wraps every incoming sample into its own individual {@link VerticalProfile} message containing a single data point.
 * When a profile is requested, it does not modify or average the data; instead, it calculates a stride to iterate
 * through the list. It returns the requested number of samples by picking evenly spaced points from the chronological
 * history.
 */
public class SubsamplingProfiler<T extends Message> implements DataProfiler<T> {

    private final ArrayList<VerticalProfile> samples = new ArrayList<>();

    public void setSample(EstimatedState estate, T sample) {

        VerticalProfile profile = new VerticalProfile();
        profile.timestamp = sample.timestamp;
        profile.numSamples = 1;

        double[] lld = WGS84Utilities.toLatLonDepth(estate);
        double lat_d = lld[0];
        double lon_d = lld[1];
        profile.lat = lat_d;
        profile.lon = lon_d;

        ProfileSample profileSample = new ProfileSample();
        profileSample.timestamp = sample.timestamp;
        profileSample.avg = sample.getFloat("value");
        profileSample.depth = Math.round(estate.depth * 10);
        profile.samples.add(profileSample);

        addSample(profile);
    }

    /**
     * @param numberSamples Number of samples to return
     * @return List of Profile samples
     */
    public ArrayList<Message> getProfile(VerticalProfile.PARAMETER type, int numberSamples) {

        // Select numberSamples equally spaced based on location of samples (lat,lon, depth)
        ArrayList<Message> result = new ArrayList<>();
        synchronized (samples) {
            if (samples.isEmpty() || numberSamples <= 0) {
                return result;
            }
        }

        synchronized (samples) {
            if (samples.size() <= numberSamples) {
                return getAllSamples(result, type);
            }
        }

        synchronized (samples) {
            double stride = (double) (samples.size() - 1) / (double) (numberSamples - 1);

            for (int i = 0; i < numberSamples; i++) {
                int idx = (int) Math.round(i * stride);

                VerticalProfile vp = samples.get(idx);
                vp.parameter = type;
                result.add(vp);
            }
        }

        return result;
    }

    @Override
    public void clearSamples() {
        samples.clear();
    }

    private void addSample(VerticalProfile profile) {
        synchronized (samples) {
            samples.add(profile);
        }
    }

    private ArrayList<Message> getAllSamples(ArrayList<Message> result, VerticalProfile.PARAMETER type) {
        for (VerticalProfile vp : samples) {
            vp.parameter = type;
            result.add(vp);
        }
        return result;
    }
}
