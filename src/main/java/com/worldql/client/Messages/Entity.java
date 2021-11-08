// automatically generated by the FlatBuffers compiler, do not modify

package com.worldql.client.Messages;

import java.nio.*;
import java.lang.*;

import com.google.flatbuffers.*;

@SuppressWarnings("unused")
public final class Entity extends Table {
  public static void ValidateVersion() { Constants.FLATBUFFERS_2_0_0(); }
  public static Entity getRootAsEntity(ByteBuffer _bb) { return getRootAsEntity(_bb, new Entity()); }
  public static Entity getRootAsEntity(ByteBuffer _bb, Entity obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { __reset(_i, _bb); }
  public Entity __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public String uuid() { int o = __offset(4); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer uuidAsByteBuffer() { return __vector_as_bytebuffer(4, 1); }
  public ByteBuffer uuidInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 4, 1); }
  public com.worldql.client.Messages.Vec3d position() { return position(new com.worldql.client.Messages.Vec3d()); }
  public com.worldql.client.Messages.Vec3d position(com.worldql.client.Messages.Vec3d obj) { int o = __offset(6); return o != 0 ? obj.__assign(o + bb_pos, bb) : null; }
  public String worldName() { int o = __offset(8); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer worldNameAsByteBuffer() { return __vector_as_bytebuffer(8, 1); }
  public ByteBuffer worldNameInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 8, 1); }
  public String data() { int o = __offset(10); return o != 0 ? __string(o + bb_pos) : null; }
  public ByteBuffer dataAsByteBuffer() { return __vector_as_bytebuffer(10, 1); }
  public ByteBuffer dataInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 10, 1); }
  public int flex(int j) { int o = __offset(12); return o != 0 ? bb.get(__vector(o) + j * 1) & 0xFF : 0; }
  public int flexLength() { int o = __offset(12); return o != 0 ? __vector_len(o) : 0; }
  public ByteVector flexVector() { return flexVector(new ByteVector()); }
  public ByteVector flexVector(ByteVector obj) { int o = __offset(12); return o != 0 ? obj.__assign(__vector(o), bb) : null; }
  public ByteBuffer flexAsByteBuffer() { return __vector_as_bytebuffer(12, 1); }
  public ByteBuffer flexInByteBuffer(ByteBuffer _bb) { return __vector_in_bytebuffer(_bb, 12, 1); }

  public static void startEntity(FlatBufferBuilder builder) { builder.startTable(5); }
  public static void addUuid(FlatBufferBuilder builder, int uuidOffset) { builder.addOffset(0, uuidOffset, 0); }
  public static void addPosition(FlatBufferBuilder builder, int positionOffset) { builder.addStruct(1, positionOffset, 0); }
  public static void addWorldName(FlatBufferBuilder builder, int worldNameOffset) { builder.addOffset(2, worldNameOffset, 0); }
  public static void addData(FlatBufferBuilder builder, int dataOffset) { builder.addOffset(3, dataOffset, 0); }
  public static void addFlex(FlatBufferBuilder builder, int flexOffset) { builder.addOffset(4, flexOffset, 0); }
  public static int createFlexVector(FlatBufferBuilder builder, byte[] data) { return builder.createByteVector(data); }
  public static int createFlexVector(FlatBufferBuilder builder, ByteBuffer data) { return builder.createByteVector(data); }
  public static void startFlexVector(FlatBufferBuilder builder, int numElems) { builder.startVector(1, numElems, 1); }
  public static int endEntity(FlatBufferBuilder builder) {
    int o = builder.endTable();
    return o;
  }

  public static final class Vector extends BaseVector {
    public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) { __reset(_vector, _element_size, _bb); return this; }

    public Entity get(int j) { return get(new Entity(), j); }
    public Entity get(Entity obj, int j) {  return obj.__assign(__indirect(__element(j), bb), bb); }
  }
}

