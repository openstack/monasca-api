/*     */ package com.hp.csbu.cc.security.cs.thrift.service;
/*     */ 
/*     */ import java.io.IOException;
/*     */ import java.io.ObjectInputStream;
/*     */ import java.io.ObjectOutputStream;
/*     */ import java.io.Serializable;
/*     */ import java.util.BitSet;
/*     */ import java.util.Collections;
/*     */ import java.util.EnumMap;
/*     */ import java.util.EnumSet;
/*     */ import java.util.HashMap;
/*     */ import java.util.Map;
/*     */ import org.apache.thrift.TBase;
/*     */ import org.apache.thrift.TBaseHelper;
/*     */ import org.apache.thrift.TException;
/*     */ import org.apache.thrift.TFieldIdEnum;
/*     */ import org.apache.thrift.meta_data.FieldMetaData;
/*     */ import org.apache.thrift.meta_data.FieldValueMetaData;
/*     */ import org.apache.thrift.protocol.TCompactProtocol;
/*     */ import org.apache.thrift.protocol.TField;
/*     */ import org.apache.thrift.protocol.TProtocol;
/*     */ import org.apache.thrift.protocol.TProtocolUtil;
/*     */ import org.apache.thrift.protocol.TStruct;
/*     */ import org.apache.thrift.protocol.TTupleProtocol;
/*     */ import org.apache.thrift.scheme.IScheme;
/*     */ import org.apache.thrift.scheme.SchemeFactory;
/*     */ import org.apache.thrift.scheme.StandardScheme;
/*     */ import org.apache.thrift.scheme.TupleScheme;
/*     */ import org.apache.thrift.transport.TIOStreamTransport;
/*     */ 
/*     */ public class ResourceException extends Exception
/*     */   implements TBase<ResourceException, ResourceException._Fields>, Serializable, Cloneable
/*     */ {
/*  36 */   private static final TStruct STRUCT_DESC = new TStruct("ResourceException");
/*     */ 
/*  38 */   private static final TField CODE_FIELD_DESC = new TField("code", (byte)8, (short)1);
/*  39 */   private static final TField MESSAGE_FIELD_DESC = new TField("message", (byte)11, (short)2);
/*  40 */   private static final TField DETAIL_FIELD_DESC = new TField("detail", (byte)11, (short)3);
/*     */ 
/*  42 */   private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap();
/*     */   public int code;
/*     */   public String message;
/*     */   public String detail;
/*     */   private static final int __CODE_ISSET_ID = 0;
/* 118 */   private BitSet __isset_bit_vector = new BitSet(1);
/*     */   public static final Map<_Fields, FieldMetaData> metaDataMap;
/*     */ 
/*     */   public ResourceException()
/*     */   {
/*     */   }
/*     */ 
/*     */   public ResourceException(int code, String message, String detail)
/*     */   {
/* 140 */     this();
/* 141 */     this.code = code;
/* 142 */     setCodeIsSet(true);
/* 143 */     this.message = message;
/* 144 */     this.detail = detail;
/*     */   }
/*     */ 
/*     */   public ResourceException(ResourceException other)
/*     */   {
/* 151 */     this.__isset_bit_vector.clear();
/* 152 */     this.__isset_bit_vector.or(other.__isset_bit_vector);
/* 153 */     this.code = other.code;
/* 154 */     if (other.isSetMessage()) {
/* 155 */       this.message = other.message;
/*     */     }
/* 157 */     if (other.isSetDetail())
/* 158 */       this.detail = other.detail;
/*     */   }
/*     */ 
/*     */   public ResourceException deepCopy()
/*     */   {
/* 163 */     return new ResourceException(this);
/*     */   }
/*     */ 
/*     */   public void clear()
/*     */   {
/* 168 */     setCodeIsSet(false);
/* 169 */     this.code = 0;
/* 170 */     this.message = null;
/* 171 */     this.detail = null;
/*     */   }
/*     */ 
/*     */   public int getCode() {
/* 175 */     return this.code;
/*     */   }
/*     */ 
/*     */   public ResourceException setCode(int code) {
/* 179 */     this.code = code;
/* 180 */     setCodeIsSet(true);
/* 181 */     return this;
/*     */   }
/*     */ 
/*     */   public void unsetCode() {
/* 185 */     this.__isset_bit_vector.clear(0);
/*     */   }
/*     */ 
/*     */   public boolean isSetCode()
/*     */   {
/* 190 */     return this.__isset_bit_vector.get(0);
/*     */   }
/*     */ 
/*     */   public void setCodeIsSet(boolean value) {
/* 194 */     this.__isset_bit_vector.set(0, value);
/*     */   }
/*     */ 
/*     */   public String getMessage() {
/* 198 */     return this.message;
/*     */   }
/*     */ 
/*     */   public ResourceException setMessage(String message) {
/* 202 */     this.message = message;
/* 203 */     return this;
/*     */   }
/*     */ 
/*     */   public void unsetMessage() {
/* 207 */     this.message = null;
/*     */   }
/*     */ 
/*     */   public boolean isSetMessage()
/*     */   {
/* 212 */     return this.message != null;
/*     */   }
/*     */ 
/*     */   public void setMessageIsSet(boolean value) {
/* 216 */     if (!value)
/* 217 */       this.message = null;
/*     */   }
/*     */ 
/*     */   public String getDetail()
/*     */   {
/* 222 */     return this.detail;
/*     */   }
/*     */ 
/*     */   public ResourceException setDetail(String detail) {
/* 226 */     this.detail = detail;
/* 227 */     return this;
/*     */   }
/*     */ 
/*     */   public void unsetDetail() {
/* 231 */     this.detail = null;
/*     */   }
/*     */ 
/*     */   public boolean isSetDetail()
/*     */   {
/* 236 */     return this.detail != null;
/*     */   }
/*     */ 
/*     */   public void setDetailIsSet(boolean value) {
/* 240 */     if (!value)
/* 241 */       this.detail = null;
/*     */   }
/*     */ 
/*     */   public void setFieldValue(_Fields field, Object value)
/*     */   {
/* 246 */     switch (field.ordinal()) {//1.$SwitchMap$com$hp$csbu$cc$security$cs$thrift$service$ResourceException$_Fields[field.ordinal()]) {
/*     */     case 1:
/* 248 */       if (value == null)
/* 249 */         unsetCode();
/*     */       else {
/* 251 */         setCode(((Integer)value).intValue());
/*     */       }
/* 253 */       break;
/*     */     case 2:
/* 256 */       if (value == null)
/* 257 */         unsetMessage();
/*     */       else {
/* 259 */         setMessage((String)value);
/*     */       }
/* 261 */       break;
/*     */     case 3:
/* 264 */       if (value == null)
/* 265 */         unsetDetail();
/*     */       else
/* 267 */         setDetail((String)value);
/*     */       break;
/*     */     }
/*     */   }
/*     */ 
/*     */   public Object getFieldValue(_Fields field)
/*     */   {
/* 275 */     switch (field.ordinal()) {//1.$SwitchMap$com$hp$csbu$cc$security$cs$thrift$service$ResourceException$_Fields[field.ordinal()]) {
/*     */     case 1:
/* 277 */       return Integer.valueOf(getCode());
/*     */     case 2:
/* 280 */       return getMessage();
/*     */     case 3:
/* 283 */       return getDetail();
/*     */     }
/*     */ 
/* 286 */     throw new IllegalStateException();
/*     */   }
/*     */ 
/*     */   public boolean isSet(_Fields field)
/*     */   {
/* 291 */     if (field == null) {
/* 292 */       throw new IllegalArgumentException();
/*     */     }
/*     */ 
/* 295 */     switch (field.ordinal()) {//1.$SwitchMap$com$hp$csbu$cc$security$cs$thrift$service$ResourceException$_Fields[field.ordinal()]) {
/*     */     case 1:
/* 297 */       return isSetCode();
/*     */     case 2:
/* 299 */       return isSetMessage();
/*     */     case 3:
/* 301 */       return isSetDetail();
/*     */     }
/* 303 */     throw new IllegalStateException();
/*     */   }
/*     */ 
/*     */   public boolean equals(Object that)
/*     */   {
/* 308 */     if (that == null)
/* 309 */       return false;
/* 310 */     if ((that instanceof ResourceException))
/* 311 */       return equals((ResourceException)that);
/* 312 */     return false;
/*     */   }
/*     */ 
/*     */   public boolean equals(ResourceException that) {
/* 316 */     if (that == null) {
/* 317 */       return false;
/*     */     }
/* 319 */     boolean this_present_code = true;
/* 320 */     boolean that_present_code = true;
/* 321 */     if ((this_present_code) || (that_present_code)) {
/* 322 */       if ((!this_present_code) || (!that_present_code))
/* 323 */         return false;
/* 324 */       if (this.code != that.code) {
/* 325 */         return false;
/*     */       }
/*     */     }
/* 328 */     boolean this_present_message = isSetMessage();
/* 329 */     boolean that_present_message = that.isSetMessage();
/* 330 */     if ((this_present_message) || (that_present_message)) {
/* 331 */       if ((!this_present_message) || (!that_present_message))
/* 332 */         return false;
/* 333 */       if (!this.message.equals(that.message)) {
/* 334 */         return false;
/*     */       }
/*     */     }
/* 337 */     boolean this_present_detail = isSetDetail();
/* 338 */     boolean that_present_detail = that.isSetDetail();
/* 339 */     if ((this_present_detail) || (that_present_detail)) {
/* 340 */       if ((!this_present_detail) || (!that_present_detail))
/* 341 */         return false;
/* 342 */       if (!this.detail.equals(that.detail)) {
/* 343 */         return false;
/*     */       }
/*     */     }
/* 346 */     return true;
/*     */   }
/*     */ 
/*     */   public int hashCode()
/*     */   {
/* 351 */     return 0;
/*     */   }
/*     */ 
/*     */   public int compareTo(ResourceException other) {
/* 355 */     if (!getClass().equals(other.getClass())) {
/* 356 */       return getClass().getName().compareTo(other.getClass().getName());
/*     */     }
/*     */ 
/* 359 */     int lastComparison = 0;
/* 360 */     ResourceException typedOther = other;
/*     */ 
/* 362 */     lastComparison = Boolean.valueOf(isSetCode()).compareTo(Boolean.valueOf(typedOther.isSetCode()));
/* 363 */     if (lastComparison != 0) {
/* 364 */       return lastComparison;
/*     */     }
/* 366 */     if (isSetCode()) {
/* 367 */       lastComparison = TBaseHelper.compareTo(this.code, typedOther.code);
/* 368 */       if (lastComparison != 0) {
/* 369 */         return lastComparison;
/*     */       }
/*     */     }
/* 372 */     lastComparison = Boolean.valueOf(isSetMessage()).compareTo(Boolean.valueOf(typedOther.isSetMessage()));
/* 373 */     if (lastComparison != 0) {
/* 374 */       return lastComparison;
/*     */     }
/* 376 */     if (isSetMessage()) {
/* 377 */       lastComparison = TBaseHelper.compareTo(this.message, typedOther.message);
/* 378 */       if (lastComparison != 0) {
/* 379 */         return lastComparison;
/*     */       }
/*     */     }
/* 382 */     lastComparison = Boolean.valueOf(isSetDetail()).compareTo(Boolean.valueOf(typedOther.isSetDetail()));
/* 383 */     if (lastComparison != 0) {
/* 384 */       return lastComparison;
/*     */     }
/* 386 */     if (isSetDetail()) {
/* 387 */       lastComparison = TBaseHelper.compareTo(this.detail, typedOther.detail);
/* 388 */       if (lastComparison != 0) {
/* 389 */         return lastComparison;
/*     */       }
/*     */     }
/* 392 */     return 0;
/*     */   }
/*     */ 
/*     */   public _Fields fieldForId(int fieldId) {
/* 396 */     return _Fields.findByThriftId(fieldId);
/*     */   }
/*     */ 
/*     */   public void read(TProtocol iprot) throws TException {
/* 400 */     ((SchemeFactory)schemes.get(iprot.getScheme())).getScheme().read(iprot, this);
/*     */   }
/*     */ 
/*     */   public void write(TProtocol oprot) throws TException {
/* 404 */     ((SchemeFactory)schemes.get(oprot.getScheme())).getScheme().write(oprot, this);
/*     */   }
/*     */ 
/*     */   public String toString()
/*     */   {
/* 409 */     StringBuilder sb = new StringBuilder("ResourceException(");
/* 410 */     boolean first = true;
/*     */ 
/* 412 */     sb.append("code:");
/* 413 */     sb.append(this.code);
/* 414 */     first = false;
/* 415 */     if (!first) sb.append(", ");
/* 416 */     sb.append("message:");
/* 417 */     if (this.message == null)
/* 418 */       sb.append("null");
/*     */     else {
/* 420 */       sb.append(this.message);
/*     */     }
/* 422 */     first = false;
/* 423 */     if (!first) sb.append(", ");
/* 424 */     sb.append("detail:");
/* 425 */     if (this.detail == null)
/* 426 */       sb.append("null");
/*     */     else {
/* 428 */       sb.append(this.detail);
/*     */     }
/* 430 */     first = false;
/* 431 */     sb.append(")");
/* 432 */     return sb.toString();
/*     */   }
/*     */ 
/*     */   public void validate() throws TException
/*     */   {
/*     */   }
/*     */ 
/*     */   private void writeObject(ObjectOutputStream out) throws IOException {
/*     */     try {
/* 441 */       write(new TCompactProtocol(new TIOStreamTransport(out)));
/*     */     } catch (TException te) {
/* 443 */       throw new IOException(te);
/*     */     }
/*     */   }
/*     */ 
/*     */   private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
/*     */     try {
/* 449 */       read(new TCompactProtocol(new TIOStreamTransport(in)));
/*     */     } catch (TException te) {
/* 451 */       throw new IOException(te);
/*     */     }
/*     */   }
/*     */ 
/*     */   static
/*     */   {
/*  44 */     schemes.put(StandardScheme.class, new ResourceExceptionStandardSchemeFactory());
/*  45 */     schemes.put(TupleScheme.class, new ResourceExceptionTupleSchemeFactory());
/*     */ 
/* 121 */     Map tmpMap = new EnumMap(_Fields.class);
/* 122 */     tmpMap.put(_Fields.CODE, new FieldMetaData("code", (byte)3, new FieldValueMetaData((byte)8)));
/*     */ 
/* 124 */     tmpMap.put(_Fields.MESSAGE, new FieldMetaData("message", (byte)3, new FieldValueMetaData((byte)11)));
/*     */ 
/* 126 */     tmpMap.put(_Fields.DETAIL, new FieldMetaData("detail", (byte)3, new FieldValueMetaData((byte)11)));
/*     */ 
/* 128 */     metaDataMap = Collections.unmodifiableMap(tmpMap);
/* 129 */     FieldMetaData.addStructMetaDataMap(ResourceException.class, metaDataMap);
/*     */   }
/*     */ 
/*     */   private static class ResourceExceptionTupleScheme extends TupleScheme<ResourceException>
/*     */   {
/*     */     public void write(TProtocol prot, ResourceException struct)
/*     */       throws TException
/*     */     {
/* 541 */       TTupleProtocol oprot = (TTupleProtocol)prot;
/* 542 */       BitSet optionals = new BitSet();
/* 543 */       if (struct.isSetCode()) {
/* 544 */         optionals.set(0);
/*     */       }
/* 546 */       if (struct.isSetMessage()) {
/* 547 */         optionals.set(1);
/*     */       }
/* 549 */       if (struct.isSetDetail()) {
/* 550 */         optionals.set(2);
/*     */       }
/* 552 */       oprot.writeBitSet(optionals, 3);
/* 553 */       if (struct.isSetCode()) {
/* 554 */         oprot.writeI32(struct.code);
/*     */       }
/* 556 */       if (struct.isSetMessage()) {
/* 557 */         oprot.writeString(struct.message);
/*     */       }
/* 559 */       if (struct.isSetDetail())
/* 560 */         oprot.writeString(struct.detail);
/*     */     }
/*     */ 
/*     */     public void read(TProtocol prot, ResourceException struct)
/*     */       throws TException
/*     */     {
/* 566 */       TTupleProtocol iprot = (TTupleProtocol)prot;
/* 567 */       BitSet incoming = iprot.readBitSet(3);
/* 568 */       if (incoming.get(0)) {
/* 569 */         struct.code = iprot.readI32();
/* 570 */         struct.setCodeIsSet(true);
/*     */       }
/* 572 */       if (incoming.get(1)) {
/* 573 */         struct.message = iprot.readString();
/* 574 */         struct.setMessageIsSet(true);
/*     */       }
/* 576 */       if (incoming.get(2)) {
/* 577 */         struct.detail = iprot.readString();
/* 578 */         struct.setDetailIsSet(true);
/*     */       }
/*     */     }
/*     */   }
/*     */ 
/*     */   private static class ResourceExceptionTupleSchemeFactory
/*     */     implements SchemeFactory
/*     */   {
/*     */     public ResourceException.ResourceExceptionTupleScheme getScheme()
/*     */     {
/* 533 */       return new ResourceException.ResourceExceptionTupleScheme();
/*     */     }
/*     */   }
/*     */ 
/*     */   private static class ResourceExceptionStandardScheme extends StandardScheme<ResourceException>
/*     */   {
/*     */     public void read(TProtocol iprot, ResourceException struct)
/*     */       throws TException
/*     */     {
/* 465 */       iprot.readStructBegin();
/*     */       while (true)
/*     */       {
/* 468 */         TField schemeField = iprot.readFieldBegin();
/* 469 */         if (schemeField.type == 0) {
/*     */           break;
/*     */         }
/* 472 */         switch (schemeField.id) {
/*     */         case 1:
/* 474 */           if (schemeField.type == 8) {
/* 475 */             struct.code = iprot.readI32();
/* 476 */             struct.setCodeIsSet(true);
/*     */           } else {
/* 478 */             TProtocolUtil.skip(iprot, schemeField.type);
/*     */           }
/* 480 */           break;
/*     */         case 2:
/* 482 */           if (schemeField.type == 11) {
/* 483 */             struct.message = iprot.readString();
/* 484 */             struct.setMessageIsSet(true);
/*     */           } else {
/* 486 */             TProtocolUtil.skip(iprot, schemeField.type);
/*     */           }
/* 488 */           break;
/*     */         case 3:
/* 490 */           if (schemeField.type == 11) {
/* 491 */             struct.detail = iprot.readString();
/* 492 */             struct.setDetailIsSet(true);
/*     */           } else {
/* 494 */             TProtocolUtil.skip(iprot, schemeField.type);
/*     */           }
/* 496 */           break;
/*     */         default:
/* 498 */           TProtocolUtil.skip(iprot, schemeField.type);
/*     */         }
/* 500 */         iprot.readFieldEnd();
/*     */       }
/* 502 */       iprot.readStructEnd();
/*     */ 
/* 505 */       struct.validate();
/*     */     }
/*     */ 
/*     */     public void write(TProtocol oprot, ResourceException struct) throws TException {
/* 509 */       struct.validate();
/*     */ 
/* 511 */       oprot.writeStructBegin(ResourceException.STRUCT_DESC);
/* 512 */       oprot.writeFieldBegin(ResourceException.CODE_FIELD_DESC);
/* 513 */       oprot.writeI32(struct.code);
/* 514 */       oprot.writeFieldEnd();
/* 515 */       if (struct.message != null) {
/* 516 */         oprot.writeFieldBegin(ResourceException.MESSAGE_FIELD_DESC);
/* 517 */         oprot.writeString(struct.message);
/* 518 */         oprot.writeFieldEnd();
/*     */       }
/* 520 */       if (struct.detail != null) {
/* 521 */         oprot.writeFieldBegin(ResourceException.DETAIL_FIELD_DESC);
/* 522 */         oprot.writeString(struct.detail);
/* 523 */         oprot.writeFieldEnd();
/*     */       }
/* 525 */       oprot.writeFieldStop();
/* 526 */       oprot.writeStructEnd();
/*     */     }
/*     */   }
/*     */ 
/*     */   private static class ResourceExceptionStandardSchemeFactory
/*     */     implements SchemeFactory
/*     */   {
/*     */     public ResourceException.ResourceExceptionStandardScheme getScheme()
/*     */     {
/* 457 */       return new ResourceException.ResourceExceptionStandardScheme();
/*     */     }
/*     */   }
/*     */ 
/*     */   public static enum _Fields
/*     */     implements TFieldIdEnum
/*     */   {
/*  54 */     CODE((short)1, "code"), 
/*  55 */     MESSAGE((short)2, "message"), 
/*  56 */     DETAIL((short)3, "detail");
/*     */ 
/*     */     private static final Map<String, _Fields> byName;
/*     */     private final short _thriftId;
/*     */     private final String _fieldName;
/*     */ 
/*     */     public static _Fields findByThriftId(int fieldId)
/*     */     {
/*  70 */       switch (fieldId) {
/*     */       case 1:
/*  72 */         return CODE;
/*     */       case 2:
/*  74 */         return MESSAGE;
/*     */       case 3:
/*  76 */         return DETAIL;
/*     */       }
/*  78 */       return null;
/*     */     }
/*     */ 
/*     */     public static _Fields findByThriftIdOrThrow(int fieldId)
/*     */     {
/*  87 */       _Fields fields = findByThriftId(fieldId);
/*  88 */       if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
/*  89 */       return fields;
/*     */     }
/*     */ 
/*     */     public static _Fields findByName(String name)
/*     */     {
/*  96 */       return (_Fields)byName.get(name);
/*     */     }
/*     */ 
/*     */     private _Fields(short thriftId, String fieldName)
/*     */     {
/* 103 */       this._thriftId = thriftId;
/* 104 */       this._fieldName = fieldName;
/*     */     }
/*     */ 
/*     */     public short getThriftFieldId() {
/* 108 */       return this._thriftId;
/*     */     }
/*     */ 
/*     */     public String getFieldName() {
/* 112 */       return this._fieldName;
/*     */     }
/*     */ 
/*     */     static
/*     */     {
/*  58 */       byName = new HashMap();
/*     */ 
/*  61 */       for (_Fields field : EnumSet.allOf(_Fields.class))
/*  62 */         byName.put(field.getFieldName(), field);
/*     */     }
/*     */   }
/*     */ }

/* Location:           /Users/johnderr/.m2/repository/com/hp/csbu/cc/CsThriftModel/1.2-SNAPSHOT/CsThriftModel-1.2-20140130.160951-1223.jar
 * Qualified Name:     com.hp.csbu.cc.security.cs.thrift.service.ResourceException
 * JD-Core Version:    0.6.2
 */