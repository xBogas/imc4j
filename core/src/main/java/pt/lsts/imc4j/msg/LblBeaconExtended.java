package pt.lsts.imc4j.msg;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.Exception;
import java.lang.String;
import java.nio.ByteBuffer;
import pt.lsts.imc4j.annotations.FieldType;
import pt.lsts.imc4j.annotations.IMCField;
import pt.lsts.imc4j.util.SerializationUtils;

/**
 * Position and configuration of an LBL transponder (beacon). The LBL transponder may be fixed or mobile, depending on TTL value.
 */
public class LblBeaconExtended extends Message {
	public static final int ID_STATIC = 204;

	/**
	 * Name/Label of the acoustic transponder.
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String beacon = "";

	/**
	 * WGS-84 Latitude coordinate.
	 */
	@FieldType(
			type = IMCField.TYPE_FP64,
			max = 1.5707963267948966,
			min = -1.5707963267948966,
			units = "rad"
	)
	public double lat = 0;

	/**
	 * WGS-84 Longitude coordinate.
	 */
	@FieldType(
			type = IMCField.TYPE_FP64,
			max = 3.141592653589793,
			min = -3.141592653589793,
			units = "rad"
	)
	public double lon = 0;

	/**
	 * The beacon's depth.
	 */
	@FieldType(
			type = IMCField.TYPE_FP32,
			units = "m"
	)
	public float depth = 0f;

	/**
	 * Interrogation channel.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT8
	)
	public int query_channel = 0;

	/**
	 * Reply channel.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT8
	)
	public int reply_channel = 0;

	/**
	 * Transponder delay.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT8,
			units = "ms"
	)
	public int transponder_delay = 0;

	/**
	 * Epoch time of this beacon configuration. For *ttl* seconds after this instant, position is estimated
	 * to vary according to *vx*, *vy* and *vz*.
	 */
	@FieldType(
			type = IMCField.TYPE_FP64,
			units = "s"
	)
	public double ctime = 0;

	/**
	 * Defines if LBL Beacon is fixed or mobile. If set to 0, LBL Beacon is fixed. Otherwise, it's the time,
	 * in seconds, that the current position is considered valid.
	 */
	@FieldType(
			type = IMCField.TYPE_UINT16,
			units = "s"
	)
	public int ttl = 0;

	/**
	 * For mobile LBL Beacons. Ground Velocity xx axis velocity component.
	 */
	@FieldType(
			type = IMCField.TYPE_FP32,
			units = "m/s"
	)
	public float vx = 0f;

	/**
	 * For mobile LBL Beacons. Ground Velocity yy axis velocity component.
	 */
	@FieldType(
			type = IMCField.TYPE_FP32,
			units = "m/s"
	)
	public float vy = 0f;

	/**
	 * For mobile LBL Beacons. Ground Velocity zz axis velocity component.
	 */
	@FieldType(
			type = IMCField.TYPE_FP32,
			units = "m/s"
	)
	public float vz = 0f;

	public String abbrev() {
		return "LblBeaconExtended";
	}

	public int mgid() {
		return 204;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			SerializationUtils.serializePlaintext(_out, beacon);
			_out.writeDouble(lat);
			_out.writeDouble(lon);
			_out.writeFloat(depth);
			_out.writeByte(query_channel);
			_out.writeByte(reply_channel);
			_out.writeByte(transponder_delay);
			_out.writeDouble(ctime);
			_out.writeShort(ttl);
			_out.writeFloat(vx);
			_out.writeFloat(vy);
			_out.writeFloat(vz);
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			beacon = SerializationUtils.deserializePlaintext(buf);
			lat = buf.getDouble();
			lon = buf.getDouble();
			depth = buf.getFloat();
			query_channel = buf.get() & 0xFF;
			reply_channel = buf.get() & 0xFF;
			transponder_delay = buf.get() & 0xFF;
			ctime = buf.getDouble();
			ttl = buf.getShort() & 0xFFFF;
			vx = buf.getFloat();
			vy = buf.getFloat();
			vz = buf.getFloat();
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
}
