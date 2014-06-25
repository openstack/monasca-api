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
/*     */ import java.util.Map.Entry;
/*     */ import org.apache.thrift.TBase;
/*     */ import org.apache.thrift.TBaseHelper;
/*     */ import org.apache.thrift.TException;
/*     */ import org.apache.thrift.TFieldIdEnum;
/*     */ import org.apache.thrift.meta_data.FieldMetaData;
/*     */ import org.apache.thrift.meta_data.FieldValueMetaData;
/*     */ import org.apache.thrift.meta_data.MapMetaData;
/*     */ import org.apache.thrift.meta_data.StructMetaData;
/*     */ import org.apache.thrift.protocol.TCompactProtocol;
/*     */ import org.apache.thrift.protocol.TField;
/*     */ import org.apache.thrift.protocol.TMap;
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
/*     */ public class SigAuthRequest
/*     */   implements TBase<SigAuthRequest, SigAuthRequest._Fields>, Serializable, Cloneable
/*     */ {
/*  34 */   private static final TStruct STRUCT_DESC = new TStruct("SigAuthRequest");
/*     */ 
/*  36 */   private static final TField CREDENTIALS_FIELD_DESC = new TField("credentials", (byte)12, (short)1);
/*  37 */   private static final TField PARAMS_FIELD_DESC = new TField("params", (byte)13, (short)2);
/*     */ 
/*  39 */   private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap();
/*     */   public SignatureCredentials credentials;
/*     */   public Map<String, String> params;
/*     */   public static final Map<_Fields, FieldMetaData> metaDataMap;
/*     */ 
/*     */   public SigAuthRequest()
/*     */   {
/*     */   }
/*     */ 
/*     */   public SigAuthRequest(SignatureCredentials credentials, Map<String, String> params)
/*     */   {
/* 130 */     this();
/* 131 */     this.credentials = credentials;
/* 132 */     this.params = params;
/*     */   }
/*     */ 
/*     */   public SigAuthRequest(SigAuthRequest other)
/*     */   {
/* 139 */     if (other.isSetCredentials()) {
/* 140 */       this.credentials = new SignatureCredentials(other.credentials);
/*     */     }
/* 142 */     if (other.isSetParams()) {
/* 143 */       Map __this__params = new HashMap();
/* 144 */       for (Map.Entry other_element : other.params.entrySet())
/*     */       {
/* 146 */         String other_element_key = (String)other_element.getKey();
/* 147 */         String other_element_value = (String)other_element.getValue();
/*     */ 
/* 149 */         String __this__params_copy_key = other_element_key;
/*     */ 
/* 151 */         String __this__params_copy_value = other_element_value;
/*     */ 
/* 153 */         __this__params.put(__this__params_copy_key, __this__params_copy_value);
/*     */       }
/* 155 */       this.params = __this__params;
/*     */     }
/*     */   }
/*     */ 
/*     */   public SigAuthRequest deepCopy() {
/* 160 */     return new SigAuthRequest(this);
/*     */   }
/*     */ 
/*     */   public void clear()
/*     */   {
/* 165 */     this.credentials = null;
/* 166 */     this.params = null;
/*     */   }
/*     */ 
/*     */   public SignatureCredentials getCredentials() {
/* 170 */     return this.credentials;
/*     */   }
/*     */ 
/*     */   public SigAuthRequest setCredentials(SignatureCredentials credentials) {
/* 174 */     this.credentials = credentials;
/* 175 */     return this;
/*     */   }
/*     */ 
/*     */   public void unsetCredentials() {
/* 179 */     this.credentials = null;
/*     */   }
/*     */ 
/*     */   public boolean isSetCredentials()
/*     */   {
/* 184 */     return this.credentials != null;
/*     */   }
/*     */ 
/*     */   public void setCredentialsIsSet(boolean value) {
/* 188 */     if (!value)
/* 189 */       this.credentials = null;
/*     */   }
/*     */ 
/*     */   public int getParamsSize()
/*     */   {
/* 194 */     return this.params == null ? 0 : this.params.size();
/*     */   }
/*     */ 
/*     */   public void putToParams(String key, String val) {
/* 198 */     if (this.params == null) {
/* 199 */       this.params = new HashMap();
/*     */     }
/* 201 */     this.params.put(key, val);
/*     */   }
/*     */ 
/*     */   public Map<String, String> getParams() {
/* 205 */     return this.params;
/*     */   }
/*     */ 
/*     */   public SigAuthRequest setParams(Map<String, String> params) {
/* 209 */     this.params = params;
/* 210 */     return this;
/*     */   }
/*     */ 
/*     */   public void unsetParams() {
/* 214 */     this.params = null;
/*     */   }
/*     */ 
/*     */   public boolean isSetParams()
/*     */   {
/* 219 */     return this.params != null;
/*     */   }
/*     */ 
/*     */   public void setParamsIsSet(boolean value) {
/* 223 */     if (!value)
/* 224 */       this.params = null;
/*     */   }
/*     */ 
/*     */   public void setFieldValue(_Fields field, Object value)
/*     */   {
/* 229 */     switch (field.ordinal()) { //1.$SwitchMap$com$hp$csbu$cc$security$cs$thrift$service$SigAuthRequest$_Fields[field.ordinal()]) {
/*     */     case 1:
/* 231 */       if (value == null)
/* 232 */         unsetCredentials();
/*     */       else {
/* 234 */         setCredentials((SignatureCredentials)value);
/*     */       }
/* 236 */       break;
/*     */     case 2:
/* 239 */       if (value == null)
/* 240 */         unsetParams();
/*     */       else
/* 242 */         setParams((Map)value);
/*     */       break;
/*     */     }
/*     */   }
/*     */ 
/*     */   public Object getFieldValue(_Fields field)
/*     */   {
/* 250 */     switch (field.ordinal()) {//1.$SwitchMap$com$hp$csbu$cc$security$cs$thrift$service$SigAuthRequest$_Fields[field.ordinal()]) {
/*     */     case 1:
/* 252 */       return getCredentials();
/*     */     case 2:
/* 255 */       return getParams();
/*     */     }
/*     */ 
/* 258 */     throw new IllegalStateException();
/*     */   }
/*     */ 
/*     */   public boolean isSet(_Fields field)
/*     */   {
/* 263 */     if (field == null) {
/* 264 */       throw new IllegalArgumentException();
/*     */     }
/*     */ 
/* 267 */     switch (field.ordinal()) {//1.$SwitchMap$com$hp$csbu$cc$security$cs$thrift$service$SigAuthRequest$_Fields[field.ordinal()]) {
/*     */     case 1:
/* 269 */       return isSetCredentials();
/*     */     case 2:
/* 271 */       return isSetParams();
/*     */     }
/* 273 */     throw new IllegalStateException();
/*     */   }
/*     */ 
/*     */   public boolean equals(Object that)
/*     */   {
/* 278 */     if (that == null)
/* 279 */       return false;
/* 280 */     if ((that instanceof SigAuthRequest))
/* 281 */       return equals((SigAuthRequest)that);
/* 282 */     return false;
/*     */   }
/*     */ 
/*     */   public boolean equals(SigAuthRequest that) {
/* 286 */     if (that == null) {
/* 287 */       return false;
/*     */     }
/* 289 */     boolean this_present_credentials = isSetCredentials();
/* 290 */     boolean that_present_credentials = that.isSetCredentials();
/* 291 */     if ((this_present_credentials) || (that_present_credentials)) {
/* 292 */       if ((!this_present_credentials) || (!that_present_credentials))
/* 293 */         return false;
/* 294 */       if (!this.credentials.equals(that.credentials)) {
/* 295 */         return false;
/*     */       }
/*     */     }
/* 298 */     boolean this_present_params = isSetParams();
/* 299 */     boolean that_present_params = that.isSetParams();
/* 300 */     if ((this_present_params) || (that_present_params)) {
/* 301 */       if ((!this_present_params) || (!that_present_params))
/* 302 */         return false;
/* 303 */       if (!this.params.equals(that.params)) {
/* 304 */         return false;
/*     */       }
/*     */     }
/* 307 */     return true;
/*     */   }
/*     */ 
/*     */   public int hashCode()
/*     */   {
/* 312 */     return 0;
/*     */   }
/*     */ 
/*     */   public int compareTo(SigAuthRequest other) {
/* 316 */     if (!getClass().equals(other.getClass())) {
/* 317 */       return getClass().getName().compareTo(other.getClass().getName());
/*     */     }
/*     */ 
/* 320 */     int lastComparison = 0;
/* 321 */     SigAuthRequest typedOther = other;
/*     */ 
/* 323 */     lastComparison = Boolean.valueOf(isSetCredentials()).compareTo(Boolean.valueOf(typedOther.isSetCredentials()));
/* 324 */     if (lastComparison != 0) {
/* 325 */       return lastComparison;
/*     */     }
/* 327 */     if (isSetCredentials()) {
/* 328 */       lastComparison = TBaseHelper.compareTo(this.credentials, typedOther.credentials);
/* 329 */       if (lastComparison != 0) {
/* 330 */         return lastComparison;
/*     */       }
/*     */     }
/* 333 */     lastComparison = Boolean.valueOf(isSetParams()).compareTo(Boolean.valueOf(typedOther.isSetParams()));
/* 334 */     if (lastComparison != 0) {
/* 335 */       return lastComparison;
/*     */     }
/* 337 */     if (isSetParams()) {
/* 338 */       lastComparison = TBaseHelper.compareTo(this.params, typedOther.params);
/* 339 */       if (lastComparison != 0) {
/* 340 */         return lastComparison;
/*     */       }
/*     */     }
/* 343 */     return 0;
/*     */   }
/*     */ 
/*     */   public _Fields fieldForId(int fieldId) {
/* 347 */     return _Fields.findByThriftId(fieldId);
/*     */   }
/*     */ 
/*     */   public void read(TProtocol iprot) throws TException {
/* 351 */     ((SchemeFactory)schemes.get(iprot.getScheme())).getScheme().read(iprot, this);
/*     */   }
/*     */ 
/*     */   public void write(TProtocol oprot) throws TException {
/* 355 */     ((SchemeFactory)schemes.get(oprot.getScheme())).getScheme().write(oprot, this);
/*     */   }
/*     */ 
/*     */   public String toString()
/*     */   {
/* 360 */     StringBuilder sb = new StringBuilder("SigAuthRequest(");
/* 361 */     boolean first = true;
/*     */ 
/* 363 */     sb.append("credentials:");
/* 364 */     if (this.credentials == null)
/* 365 */       sb.append("null");
/*     */     else {
/* 367 */       sb.append(this.credentials);
/*     */     }
/* 369 */     first = false;
/* 370 */     if (!first) sb.append(", ");
/* 371 */     sb.append("params:");
/* 372 */     if (this.params == null)
/* 373 */       sb.append("null");
/*     */     else {
/* 375 */       sb.append(this.params);
/*     */     }
/* 377 */     first = false;
/* 378 */     sb.append(")");
/* 379 */     return sb.toString();
/*     */   }
/*     */ 
/*     */   public void validate() throws TException
/*     */   {
/*     */   }
/*     */ 
/*     */   private void writeObject(ObjectOutputStream out) throws IOException {
/*     */     try {
/* 388 */       write(new TCompactProtocol(new TIOStreamTransport(out)));
/*     */     } catch (TException te) {
/* 390 */       throw new IOException(te);
/*     */     }
/*     */   }
/*     */ 
/*     */   private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
/*     */     try {
/* 396 */       read(new TCompactProtocol(new TIOStreamTransport(in)));
/*     */     } catch (TException te) {
/* 398 */       throw new IOException(te);
/*     */     }
/*     */   }
/*     */ 
/*     */   static
/*     */   {
/*  41 */     schemes.put(StandardScheme.class, new SigAuthRequestStandardSchemeFactory());
/*  42 */     schemes.put(TupleScheme.class, new SigAuthRequestTupleSchemeFactory());
/*     */ 
/* 112 */     Map tmpMap = new EnumMap(_Fields.class);
/* 113 */     tmpMap.put(_Fields.CREDENTIALS, new FieldMetaData("credentials", (byte)3, new StructMetaData((byte)12, SignatureCredentials.class)));
/*     */ 
/* 115 */     tmpMap.put(_Fields.PARAMS, new FieldMetaData("params", (byte)3, new MapMetaData((byte)13, new FieldValueMetaData((byte)11), new FieldValueMetaData((byte)11))));
/*     */ 
/* 119 */     metaDataMap = Collections.unmodifiableMap(tmpMap);
/* 120 */     FieldMetaData.addStructMetaDataMap(SigAuthRequest.class, metaDataMap);
/*     */   }
/*     */ 
/*     */   private static class SigAuthRequestTupleScheme extends TupleScheme<SigAuthRequest>
/*     */   {
/*     */     public void write(TProtocol prot, SigAuthRequest struct)
/*     */       throws TException
/*     */     {
/* 498 */       TTupleProtocol oprot = (TTupleProtocol)prot;
/* 499 */       BitSet optionals = new BitSet();
/* 500 */       if (struct.isSetCredentials()) {
/* 501 */         optionals.set(0);
/*     */       }
/* 503 */       if (struct.isSetParams()) {
/* 504 */         optionals.set(1);
/*     */       }
/* 506 */       oprot.writeBitSet(optionals, 2);
/* 507 */       if (struct.isSetCredentials()) {
/* 508 */         struct.credentials.write(oprot);
/*     */       }
/* 510 */       if (struct.isSetParams())
/*     */       {
/* 512 */         oprot.writeI32(struct.params.size());
/* 513 */         for (Map.Entry _iter13 : struct.params.entrySet())
/*     */         {
/* 515 */           oprot.writeString((String)_iter13.getKey());
/* 516 */           oprot.writeString((String)_iter13.getValue());
/*     */         }
/*     */       }
/*     */     }
/*     */ 
/*     */     public void read(TProtocol prot, SigAuthRequest struct)
/*     */       throws TException
/*     */     {
/* 524 */       TTupleProtocol iprot = (TTupleProtocol)prot;
/* 525 */       BitSet incoming = iprot.readBitSet(2);
/* 526 */       if (incoming.get(0)) {
/* 527 */         struct.credentials = new SignatureCredentials();
/* 528 */         struct.credentials.read(iprot);
/* 529 */         struct.setCredentialsIsSet(true);
/*     */       }
/* 531 */       if (incoming.get(1))
/*     */       {
/* 533 */         TMap _map14 = new TMap((byte)11, (byte)11, iprot.readI32());
/* 534 */         struct.params = new HashMap(2 * _map14.size);
/* 535 */         for (int _i15 = 0; _i15 < _map14.size; _i15++)
/*     */         {
/* 539 */           String _key16 = iprot.readString();
/* 540 */           String _val17 = iprot.readString();
/* 541 */           struct.params.put(_key16, _val17);
/*     */         }
/*     */ 
/* 544 */         struct.setParamsIsSet(true);
/*     */       }
/*     */     }
/*     */   }
/*     */ 
/*     */   private static class SigAuthRequestTupleSchemeFactory
/*     */     implements SchemeFactory
/*     */   {
/*     */     public SigAuthRequest.SigAuthRequestTupleScheme getScheme()
/*     */     {
/* 490 */       return new SigAuthRequest.SigAuthRequestTupleScheme();
/*     */     }
/*     */   }
/*     */ 
/*     */   private static class SigAuthRequestStandardScheme extends StandardScheme<SigAuthRequest>
/*     */   {
/*     */     public void read(TProtocol iprot, SigAuthRequest struct)
/*     */       throws TException
/*     */     {
/* 412 */       iprot.readStructBegin();
/*     */       while (true)
/*     */       {
/* 415 */         TField schemeField = iprot.readFieldBegin();
/* 416 */         if (schemeField.type == 0) {
/*     */           break;
/*     */         }
/* 419 */         switch (schemeField.id) {
/*     */         case 1:
/* 421 */           if (schemeField.type == 12) {
/* 422 */             struct.credentials = new SignatureCredentials();
/* 423 */             struct.credentials.read(iprot);
/* 424 */             struct.setCredentialsIsSet(true);
/*     */           } else {
/* 426 */             TProtocolUtil.skip(iprot, schemeField.type);
/*     */           }
/* 428 */           break;
/*     */         case 2:
/* 430 */           if (schemeField.type == 13)
/*     */           {
/* 432 */             TMap _map8 = iprot.readMapBegin();
/* 433 */             struct.params = new HashMap(2 * _map8.size);
/* 434 */             for (int _i9 = 0; _i9 < _map8.size; _i9++)
/*     */             {
/* 438 */               String _key10 = iprot.readString();
/* 439 */               String _val11 = iprot.readString();
/* 440 */               struct.params.put(_key10, _val11);
/*     */             }
/* 442 */             iprot.readMapEnd();
/*     */ 
/* 444 */             struct.setParamsIsSet(true);
/*     */           } else {
/* 446 */             TProtocolUtil.skip(iprot, schemeField.type);
/*     */           }
/* 448 */           break;
/*     */         default:
/* 450 */           TProtocolUtil.skip(iprot, schemeField.type);
/*     */         }
/* 452 */         iprot.readFieldEnd();
/*     */       }
/* 454 */       iprot.readStructEnd();
/*     */ 
/* 457 */       struct.validate();
/*     */     }
/*     */ 
/*     */     public void write(TProtocol oprot, SigAuthRequest struct) throws TException {
/* 461 */       struct.validate();
/*     */ 
/* 463 */       oprot.writeStructBegin(SigAuthRequest.STRUCT_DESC);
/* 464 */       if (struct.credentials != null) {
/* 465 */         oprot.writeFieldBegin(SigAuthRequest.CREDENTIALS_FIELD_DESC);
/* 466 */         struct.credentials.write(oprot);
/* 467 */         oprot.writeFieldEnd();
/*     */       }
/* 469 */       if (struct.params != null) {
/* 470 */         oprot.writeFieldBegin(SigAuthRequest.PARAMS_FIELD_DESC);
/*     */ 
/* 472 */         oprot.writeMapBegin(new TMap((byte)11, (byte)11, struct.params.size()));
/* 473 */         for (Map.Entry _iter12 : struct.params.entrySet())
/*     */         {
/* 475 */           oprot.writeString((String)_iter12.getKey());
/* 476 */           oprot.writeString((String)_iter12.getValue());
/*     */         }
/* 478 */         oprot.writeMapEnd();
/*     */ 
/* 480 */         oprot.writeFieldEnd();
/*     */       }
/* 482 */       oprot.writeFieldStop();
/* 483 */       oprot.writeStructEnd();
/*     */     }
/*     */   }
/*     */ 
/*     */   private static class SigAuthRequestStandardSchemeFactory
/*     */     implements SchemeFactory
/*     */   {
/*     */     public SigAuthRequest.SigAuthRequestStandardScheme getScheme()
/*     */     {
/* 404 */       return new SigAuthRequest.SigAuthRequestStandardScheme();
/*     */     }
/*     */   }
/*     */ 
/*     */   public static enum _Fields
/*     */     implements TFieldIdEnum
/*     */   {
/*  50 */     CREDENTIALS((short)1, "credentials"), 
/*  51 */     PARAMS((short)2, "params");
/*     */ 
/*     */     private static final Map<String, _Fields> byName;
/*     */     private final short _thriftId;
/*     */     private final String _fieldName;
/*     */ 
/*     */     public static _Fields findByThriftId(int fieldId)
/*     */     {
/*  65 */       switch (fieldId) {
/*     */       case 1:
/*  67 */         return CREDENTIALS;
/*     */       case 2:
/*  69 */         return PARAMS;
/*     */       }
/*  71 */       return null;
/*     */     }
/*     */ 
/*     */     public static _Fields findByThriftIdOrThrow(int fieldId)
/*     */     {
/*  80 */       _Fields fields = findByThriftId(fieldId);
/*  81 */       if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
/*  82 */       return fields;
/*     */     }
/*     */ 
/*     */     public static _Fields findByName(String name)
/*     */     {
/*  89 */       return (_Fields)byName.get(name);
/*     */     }
/*     */ 
/*     */     private _Fields(short thriftId, String fieldName)
/*     */     {
/*  96 */       this._thriftId = thriftId;
/*  97 */       this._fieldName = fieldName;
/*     */     }
/*     */ 
/*     */     public short getThriftFieldId() {
/* 101 */       return this._thriftId;
/*     */     }
/*     */ 
/*     */     public String getFieldName() {
/* 105 */       return this._fieldName;
/*     */     }
/*     */ 
/*     */     static
/*     */     {
/*  53 */       byName = new HashMap();
/*     */ 
/*  56 */       for (_Fields field : EnumSet.allOf(_Fields.class))
/*  57 */         byName.put(field.getFieldName(), field);
/*     */     }
/*     */   }
/*     */ }

/* Location:           /Users/johnderr/.m2/repository/com/hp/csbu/cc/CsThriftModel/1.2-SNAPSHOT/CsThriftModel-1.2-20140130.160951-1223.jar
 * Qualified Name:     com.hp.csbu.cc.security.cs.thrift.service.SigAuthRequest
 * JD-Core Version:    0.6.2
 */