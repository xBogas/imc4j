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
 * Message containing static or dynamic AIS data received onboard the vehicle.
 */
public class AisInfo extends Message {
	public static final int ID_STATIC = 912;

	/**
	 * Integer indicating the message type: 1,2,3,5, ...
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String msg_type = "";

	/**
	 * The class of a sensor tells the type of sensor originating this message. It will determine how the sensor is to be shown and (optionally) how the custom data (tuplelist) is to be interpreted.
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String sensor_class = "";

	/**
	 * An unique string that identifies the sensor/vessel.
	 */
	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String mmsi = "";

	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String callsign = "";

	@FieldType(
			type = IMCField.TYPE_PLAINTEXT
	)
	public String name = "";

	@FieldType(
			type = IMCField.TYPE_UINT8
	)
	public int nav_status = 0;

	@FieldType(
			type = IMCField.TYPE_UINT8
	)
	public int type_and_cargo = 0;

	@FieldType(
			type = IMCField.TYPE_FP64,
			units = "rad"
	)
	public double lat = 0;

	@FieldType(
			type = IMCField.TYPE_FP64,
			units = "rad"
	)
	public double lon = 0;

	@FieldType(
			type = IMCField.TYPE_FP32,
			units = "°"
	)
	public float course = 0f;

	@FieldType(
			type = IMCField.TYPE_FP32,
			units = "kn"
	)
	public float speed = 0f;

	/**
	 * Distance to own vehicle.
	 */
	@FieldType(
			type = IMCField.TYPE_FP32,
			units = "m"
	)
	public float dist = 0f;

	/**
	 * Size of the vehicle (length = A + B, width = C + D)
	 */
	@FieldType(
			type = IMCField.TYPE_FP32,
			units = "m"
	)
	public float a = 0f;

	@FieldType(
			type = IMCField.TYPE_FP32,
			units = "m"
	)
	public float b = 0f;

	@FieldType(
			type = IMCField.TYPE_FP32,
			units = "m"
	)
	public float c = 0f;

	@FieldType(
			type = IMCField.TYPE_FP32,
			units = "m"
	)
	public float d = 0f;

	@FieldType(
			type = IMCField.TYPE_FP32,
			units = "m"
	)
	public float draught = 0f;

	public String abbrev() {
		return "AisInfo";
	}

	public int mgid() {
		return 912;
	}

	public byte[] serializeFields() {
		try {
			ByteArrayOutputStream _data = new ByteArrayOutputStream();
			DataOutputStream _out = new DataOutputStream(_data);
			SerializationUtils.serializePlaintext(_out, msg_type);
			SerializationUtils.serializePlaintext(_out, sensor_class);
			SerializationUtils.serializePlaintext(_out, mmsi);
			SerializationUtils.serializePlaintext(_out, callsign);
			SerializationUtils.serializePlaintext(_out, name);
			_out.writeByte(nav_status);
			_out.writeByte(type_and_cargo);
			_out.writeDouble(lat);
			_out.writeDouble(lon);
			_out.writeFloat(course);
			_out.writeFloat(speed);
			_out.writeFloat(dist);
			_out.writeFloat(a);
			_out.writeFloat(b);
			_out.writeFloat(c);
			_out.writeFloat(d);
			_out.writeFloat(draught);
			return _data.toByteArray();
		}
		catch (IOException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	public void deserializeFields(ByteBuffer buf) throws IOException {
		try {
			msg_type = SerializationUtils.deserializePlaintext(buf);
			sensor_class = SerializationUtils.deserializePlaintext(buf);
			mmsi = SerializationUtils.deserializePlaintext(buf);
			callsign = SerializationUtils.deserializePlaintext(buf);
			name = SerializationUtils.deserializePlaintext(buf);
			nav_status = buf.get() & 0xFF;
			type_and_cargo = buf.get() & 0xFF;
			lat = buf.getDouble();
			lon = buf.getDouble();
			course = buf.getFloat();
			speed = buf.getFloat();
			dist = buf.getFloat();
			a = buf.getFloat();
			b = buf.getFloat();
			c = buf.getFloat();
			d = buf.getFloat();
			draught = buf.getFloat();
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}
}
