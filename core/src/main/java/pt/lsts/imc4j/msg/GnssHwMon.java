package pt.lsts.imc4j.msg;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.String;
import java.nio.ByteBuffer;
import pt.lsts.imc4j.annotations.FieldType;
import pt.lsts.imc4j.annotations.IMCField;

/**
 * GNSS debug information
 */
public class GnssHwMon extends Message {
	public static final int ID_STATIC = 2024;

	/**
	 * Probability of jamming, provided by the GNSS jamming indicator
	 */
	@FieldType(
			type = IMCField.TYPE_UINT8,
			max = 100,
			units = "%"
	)
	public int jamming_prob = 0;

	/**
	 * Jamming Status
	 */
	@FieldType(
			type = IMCField.TYPE_UINT8,
			units = "Enumerated"
	)
	public JAM_STAT jam_stat = JAM_STAT.values()[0];

	/**
	 * RF noise level as seen by the GNSS core
	 */
	@FieldType(
			type = IMCField.TYPE_UINT16
	)
	public int rf_noise = 0;

	/**
	 * Status of the antenna
	 */
	@FieldType(
			type = IMCField.TYPE_UINT8,
			units = "Enumerated"
	)
	public ANT_STAT ant_stat = ANT_STAT.values()[0];

	/**
	 * Status of the antenna power
	 */
	@FieldType(
			type = IMCField.TYPE_UINT8,
			units = "Enumerated"
	)
	public ANT_PWR ant_pwr = ANT_PWR.values()[0];

	public String abbrev() {
		return "GnssHwMon";
	}

	public int mgid() {
		return 2024;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			_out.writeByte(jamming_prob);
			_out.writeByte((int)(jam_stat != null? jam_stat.value() : 0));
			_out.writeShort(rf_noise);
			_out.writeByte((int)(ant_stat != null? ant_stat.value() : 0));
			_out.writeByte((int)(ant_pwr != null? ant_pwr.value() : 0));
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			jamming_prob = buf.get() & 0xFF;
			jam_stat = JAM_STAT.valueOf(buf.get() & 0xFF);
			rf_noise = buf.getShort() & 0xFFFF;
			ant_stat = ANT_STAT.valueOf(buf.get() & 0xFF);
			ant_pwr = ANT_PWR.valueOf(buf.get() & 0xFF);
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}

	public enum JAM_STAT {
		JS_UNKNOWN(0l),

		JS_OK(1l),

		JS_WAR(2l),

		JS_CRIT(3l);

		protected long value;

		JAM_STAT(long value) {
			this.value = value;
		}

		long value() {
			return value;
		}

		public static JAM_STAT valueOf(long value) throws IllegalArgumentException {
			for (JAM_STAT v : JAM_STAT.values()) {
				if (v.value == value) {
					return v;
				}
			}
			throw new IllegalArgumentException("Invalid value for JAM_STAT: "+value);
		}
	}

	public enum ANT_STAT {
		AS_INIT(0l),

		AS_UNKNOWN(1l),

		AS_OK(2l),

		AS_SHORT(3l),

		AS_OPEN(4l);

		protected long value;

		ANT_STAT(long value) {
			this.value = value;
		}

		long value() {
			return value;
		}

		public static ANT_STAT valueOf(long value) throws IllegalArgumentException {
			for (ANT_STAT v : ANT_STAT.values()) {
				if (v.value == value) {
					return v;
				}
			}
			throw new IllegalArgumentException("Invalid value for ANT_STAT: "+value);
		}
	}

	public enum ANT_PWR {
		AP_OFF(0l),

		AP_ON(1l),

		AP_UNKNOWN(2l);

		protected long value;

		ANT_PWR(long value) {
			this.value = value;
		}

		long value() {
			return value;
		}

		public static ANT_PWR valueOf(long value) throws IllegalArgumentException {
			for (ANT_PWR v : ANT_PWR.values()) {
				if (v.value == value) {
					return v;
				}
			}
			throw new IllegalArgumentException("Invalid value for ANT_PWR: "+value);
		}
	}
}
