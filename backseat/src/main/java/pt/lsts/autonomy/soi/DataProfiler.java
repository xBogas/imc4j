package pt.lsts.autonomy.soi;

import pt.lsts.imc4j.msg.EstimatedState;
import pt.lsts.imc4j.msg.Message;
import pt.lsts.imc4j.msg.VerticalProfile;

import java.util.ArrayList;

/**
 * Defines the interface for collecting data samples and generating data profiles.
 * Implementations of this interface are responsible for accumulating incoming sensor
 * messages alongside their corresponding {@link EstimatedState} (which provides position,
 * and attitude). They then process this collected data to generate a curated
 * list of {@link VerticalProfile} messages for transmission or storage.
 *
 * @param <T> The IMC Message type being sampled (e.g., Salinity, Temperature).
 */
public interface DataProfiler<T extends Message> {

    /**
     * @param estate current {@link EstimatedState} message
     * @param sample value message to store
     */
    void setSample(EstimatedState estate, T sample);

    /**
     * @param param  The parameter type (e.g., PROF_SALINITY)
     * @param number Number of samples or bins to return
     * @return List of generated profile messages
     */
    ArrayList<Message> getProfile(VerticalProfile.PARAMETER param, int number);

}
