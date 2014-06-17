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
/*     */ public class Token
/*     */   implements TBase<Token, Token._Fields>, Serializable, Cloneable
/*     */ {
/*  34 */   private static final TStruct STRUCT_DESC = new TStruct("Token");
/*     */ 
/*  36 */   private static final TField EXPIRES_FIELD_DESC = new TField("expires", (byte)11, (short)1);
/*  37 */   private static final TField ID_FIELD_DESC = new TField("id", (byte)11, (short)2);
/*  38 */   private static final TField TENANT_ID_FIELD_DESC = new TField("tenantId", (byte)11, (short)3);
/*     */ 
/*  40 */   private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap();
/*     */   public String expires;
/*     */   public String id;
/*     */   public String tenantId;
/* 115 */   private _Fields[] optionals = { _Fields.TENANT_ID };
/*     */   public static final Map<_Fields, FieldMetaData> metaDataMap;
/*     */ 
/*     */   public Token()
/*     */   {
/*     */   }
/*     */ 
/*     */   public Token(String expires, String id)
/*     */   {
/* 136 */     this();
/* 137 */     this.expires = expires;
/* 138 */     this.id = id;
/*     */   }
/*     */ 
/*     */   public Token(Token other)
/*     */   {
/* 145 */     if (other.isSetExpires()) {
/* 146 */       this.expires = other.expires;
/*     */     }
/* 148 */     if (other.isSetId()) {
/* 149 */       this.id = other.id;
/*     */     }
/* 151 */     if (other.isSetTenantId())
/* 152 */       this.tenantId = other.tenantId;
/*     */   }
/*     */ 
/*     */   public Token deepCopy()
/*     */   {
/* 157 */     return new Token(this);
/*     */   }
/*     */ 
/*     */   public void clear()
/*     */   {
/* 162 */     this.expires = null;
/* 163 */     this.id = null;
/* 164 */     this.tenantId = null;
/*     */   }
/*     */ 
/*     */   public String getExpires() {
/* 168 */     return this.expires;
/*     */   }
/*     */ 
/*     */   public Token setExpires(String expires) {
/* 172 */     this.expires = expires;
/* 173 */     return this;
/*     */   }
/*     */ 
/*     */   public void unsetExpires() {
/* 177 */     this.expires = null;
/*     */   }
/*     */ 
/*     */   public boolean isSetExpires()
/*     */   {
/* 182 */     return this.expires != null;
/*     */   }
/*     */ 
/*     */   public void setExpiresIsSet(boolean value) {
/* 186 */     if (!value)
/* 187 */       this.expires = null;
/*     */   }
/*     */ 
/*     */   public String getId()
/*     */   {
/* 192 */     return this.id;
/*     */   }
/*     */ 
/*     */   public Token setId(String id) {
/* 196 */     this.id = id;
/* 197 */     return this;
/*     */   }
/*     */ 
/*     */   public void unsetId() {
/* 201 */     this.id = null;
/*     */   }
/*     */ 
/*     */   public boolean isSetId()
/*     */   {
/* 206 */     return this.id != null;
/*     */   }
/*     */ 
/*     */   public void setIdIsSet(boolean value) {
/* 210 */     if (!value)
/* 211 */       this.id = null;
/*     */   }
/*     */ 
/*     */   public String getTenantId()
/*     */   {
/* 216 */     return this.tenantId;
/*     */   }
/*     */ 
/*     */   public Token setTenantId(String tenantId) {
/* 220 */     this.tenantId = tenantId;
/* 221 */     return this;
/*     */   }
/*     */ 
/*     */   public void unsetTenantId() {
/* 225 */     this.tenantId = null;
/*     */   }
/*     */ 
/*     */   public boolean isSetTenantId()
/*     */   {
/* 230 */     return this.tenantId != null;
/*     */   }
/*     */ 
/*     */   public void setTenantIdIsSet(boolean value) {
/* 234 */     if (!value)
/* 235 */       this.tenantId = null;
/*     */   }
/*     */ 
/*     */   public void setFieldValue(_Fields field, Object value)
/*     */   {
/* 240 */     switch (field.ordinal()) {//1.$SwitchMap$com$hp$csbu$cc$security$cs$thrift$service$Token$_Fields[field.ordinal()]) {
/*     */     case 1:
/* 242 */       if (value == null)
/* 243 */         unsetExpires();
/*     */       else {
/* 245 */         setExpires((String)value);
/*     */       }
/* 247 */       break;
/*     */     case 2:
/* 250 */       if (value == null)
/* 251 */         unsetId();
/*     */       else {
/* 253 */         setId((String)value);
/*     */       }
/* 255 */       break;
/*     */     case 3:
/* 258 */       if (value == null)
/* 259 */         unsetTenantId();
/*     */       else
/* 261 */         setTenantId((String)value);
/*     */       break;
/*     */     }
/*     */   }
/*     */ 
/*     */   public Object getFieldValue(_Fields field)
/*     */   {
/* 269 */     switch (field.ordinal()) {//1.$SwitchMap$com$hp$csbu$cc$security$cs$thrift$service$Token$_Fields[field.ordinal()]) {
/*     */     case 1:
/* 271 */       return getExpires();
/*     */     case 2:
/* 274 */       return getId();
/*     */     case 3:
/* 277 */       return getTenantId();
/*     */     }
/*     */ 
/* 280 */     throw new IllegalStateException();
/*     */   }
/*     */ 
/*     */   public boolean isSet(_Fields field)
/*     */   {
/* 285 */     if (field == null) {
/* 286 */       throw new IllegalArgumentException();
/*     */     }
/*     */ 
/* 289 */     switch (field.ordinal()) { //1.$SwitchMap$com$hp$csbu$cc$security$cs$thrift$service$Token$_Fields[field.ordinal()]) {
/*     */     case 1:
/* 291 */       return isSetExpires();
/*     */     case 2:
/* 293 */       return isSetId();
/*     */     case 3:
/* 295 */       return isSetTenantId();
/*     */     }
/* 297 */     throw new IllegalStateException();
/*     */   }
/*     */ 
/*     */   public boolean equals(Object that)
/*     */   {
/* 302 */     if (that == null)
/* 303 */       return false;
/* 304 */     if ((that instanceof Token))
/* 305 */       return equals((Token)that);
/* 306 */     return false;
/*     */   }
/*     */ 
/*     */   public boolean equals(Token that) {
/* 310 */     if (that == null) {
/* 311 */       return false;
/*     */     }
/* 313 */     boolean this_present_expires = isSetExpires();
/* 314 */     boolean that_present_expires = that.isSetExpires();
/* 315 */     if ((this_present_expires) || (that_present_expires)) {
/* 316 */       if ((!this_present_expires) || (!that_present_expires))
/* 317 */         return false;
/* 318 */       if (!this.expires.equals(that.expires)) {
/* 319 */         return false;
/*     */       }
/*     */     }
/* 322 */     boolean this_present_id = isSetId();
/* 323 */     boolean that_present_id = that.isSetId();
/* 324 */     if ((this_present_id) || (that_present_id)) {
/* 325 */       if ((!this_present_id) || (!that_present_id))
/* 326 */         return false;
/* 327 */       if (!this.id.equals(that.id)) {
/* 328 */         return false;
/*     */       }
/*     */     }
/* 331 */     boolean this_present_tenantId = isSetTenantId();
/* 332 */     boolean that_present_tenantId = that.isSetTenantId();
/* 333 */     if ((this_present_tenantId) || (that_present_tenantId)) {
/* 334 */       if ((!this_present_tenantId) || (!that_present_tenantId))
/* 335 */         return false;
/* 336 */       if (!this.tenantId.equals(that.tenantId)) {
/* 337 */         return false;
/*     */       }
/*     */     }
/* 340 */     return true;
/*     */   }
/*     */ 
/*     */   public int hashCode()
/*     */   {
/* 345 */     return 0;
/*     */   }
/*     */ 
/*     */   public int compareTo(Token other) {
/* 349 */     if (!getClass().equals(other.getClass())) {
/* 350 */       return getClass().getName().compareTo(other.getClass().getName());
/*     */     }
/*     */ 
/* 353 */     int lastComparison = 0;
/* 354 */     Token typedOther = other;
/*     */ 
/* 356 */     lastComparison = Boolean.valueOf(isSetExpires()).compareTo(Boolean.valueOf(typedOther.isSetExpires()));
/* 357 */     if (lastComparison != 0) {
/* 358 */       return lastComparison;
/*     */     }
/* 360 */     if (isSetExpires()) {
/* 361 */       lastComparison = TBaseHelper.compareTo(this.expires, typedOther.expires);
/* 362 */       if (lastComparison != 0) {
/* 363 */         return lastComparison;
/*     */       }
/*     */     }
/* 366 */     lastComparison = Boolean.valueOf(isSetId()).compareTo(Boolean.valueOf(typedOther.isSetId()));
/* 367 */     if (lastComparison != 0) {
/* 368 */       return lastComparison;
/*     */     }
/* 370 */     if (isSetId()) {
/* 371 */       lastComparison = TBaseHelper.compareTo(this.id, typedOther.id);
/* 372 */       if (lastComparison != 0) {
/* 373 */         return lastComparison;
/*     */       }
/*     */     }
/* 376 */     lastComparison = Boolean.valueOf(isSetTenantId()).compareTo(Boolean.valueOf(typedOther.isSetTenantId()));
/* 377 */     if (lastComparison != 0) {
/* 378 */       return lastComparison;
/*     */     }
/* 380 */     if (isSetTenantId()) {
/* 381 */       lastComparison = TBaseHelper.compareTo(this.tenantId, typedOther.tenantId);
/* 382 */       if (lastComparison != 0) {
/* 383 */         return lastComparison;
/*     */       }
/*     */     }
/* 386 */     return 0;
/*     */   }
/*     */ 
/*     */   public _Fields fieldForId(int fieldId) {
/* 390 */     return _Fields.findByThriftId(fieldId);
/*     */   }
/*     */ 
/*     */   public void read(TProtocol iprot) throws TException {
/* 394 */     ((SchemeFactory)schemes.get(iprot.getScheme())).getScheme().read(iprot, this);
/*     */   }
/*     */ 
/*     */   public void write(TProtocol oprot) throws TException {
/* 398 */     ((SchemeFactory)schemes.get(oprot.getScheme())).getScheme().write(oprot, this);
/*     */   }
/*     */ 
/*     */   public String toString()
/*     */   {
/* 403 */     StringBuilder sb = new StringBuilder("Token(");
/* 404 */     boolean first = true;
/*     */ 
/* 406 */     sb.append("expires:");
/* 407 */     if (this.expires == null)
/* 408 */       sb.append("null");
/*     */     else {
/* 410 */       sb.append(this.expires);
/*     */     }
/* 412 */     first = false;
/* 413 */     if (!first) sb.append(", ");
/* 414 */     sb.append("id:");
/* 415 */     if (this.id == null)
/* 416 */       sb.append("null");
/*     */     else {
/* 418 */       sb.append(this.id);
/*     */     }
/* 420 */     first = false;
/* 421 */     if (isSetTenantId()) {
/* 422 */       if (!first) sb.append(", ");
/* 423 */       sb.append("tenantId:");
/* 424 */       if (this.tenantId == null)
/* 425 */         sb.append("null");
/*     */       else {
/* 427 */         sb.append(this.tenantId);
/*     */       }
/* 429 */       first = false;
/*     */     }
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
/*  42 */     schemes.put(StandardScheme.class, new TokenStandardSchemeFactory());
/*  43 */     schemes.put(TupleScheme.class, new TokenTupleSchemeFactory());
/*     */ 
/* 118 */     Map tmpMap = new EnumMap(_Fields.class);
/* 119 */     tmpMap.put(_Fields.EXPIRES, new FieldMetaData("expires", (byte)3, new FieldValueMetaData((byte)11)));
/*     */ 
/* 121 */     tmpMap.put(_Fields.ID, new FieldMetaData("id", (byte)3, new FieldValueMetaData((byte)11)));
/*     */ 
/* 123 */     tmpMap.put(_Fields.TENANT_ID, new FieldMetaData("tenantId", (byte)2, new FieldValueMetaData((byte)11)));
/*     */ 
/* 125 */     metaDataMap = Collections.unmodifiableMap(tmpMap);
/* 126 */     FieldMetaData.addStructMetaDataMap(Token.class, metaDataMap);
/*     */   }
/*     */ 
/*     */   private static class TokenTupleScheme extends TupleScheme<Token>
/*     */   {
/*     */     public void write(TProtocol prot, Token struct)
/*     */       throws TException
/*     */     {
/* 545 */       TTupleProtocol oprot = (TTupleProtocol)prot;
/* 546 */       BitSet optionals = new BitSet();
/* 547 */       if (struct.isSetExpires()) {
/* 548 */         optionals.set(0);
/*     */       }
/* 550 */       if (struct.isSetId()) {
/* 551 */         optionals.set(1);
/*     */       }
/* 553 */       if (struct.isSetTenantId()) {
/* 554 */         optionals.set(2);
/*     */       }
/* 556 */       oprot.writeBitSet(optionals, 3);
/* 557 */       if (struct.isSetExpires()) {
/* 558 */         oprot.writeString(struct.expires);
/*     */       }
/* 560 */       if (struct.isSetId()) {
/* 561 */         oprot.writeString(struct.id);
/*     */       }
/* 563 */       if (struct.isSetTenantId())
/* 564 */         oprot.writeString(struct.tenantId);
/*     */     }
/*     */ 
/*     */     public void read(TProtocol prot, Token struct)
/*     */       throws TException
/*     */     {
/* 570 */       TTupleProtocol iprot = (TTupleProtocol)prot;
/* 571 */       BitSet incoming = iprot.readBitSet(3);
/* 572 */       if (incoming.get(0)) {
/* 573 */         struct.expires = iprot.readString();
/* 574 */         struct.setExpiresIsSet(true);
/*     */       }
/* 576 */       if (incoming.get(1)) {
/* 577 */         struct.id = iprot.readString();
/* 578 */         struct.setIdIsSet(true);
/*     */       }
/* 580 */       if (incoming.get(2)) {
/* 581 */         struct.tenantId = iprot.readString();
/* 582 */         struct.setTenantIdIsSet(true);
/*     */       }
/*     */     }
/*     */   }
/*     */ 
/*     */   private static class TokenTupleSchemeFactory
/*     */     implements SchemeFactory
/*     */   {
/*     */     public Token.TokenTupleScheme getScheme()
/*     */     {
/* 537 */       return new Token.TokenTupleScheme();
/*     */     }
/*     */   }
/*     */ 
/*     */   private static class TokenStandardScheme extends StandardScheme<Token>
/*     */   {
/*     */     public void read(TProtocol iprot, Token struct)
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
/* 474 */           if (schemeField.type == 11) {
/* 475 */             struct.expires = iprot.readString();
/* 476 */             struct.setExpiresIsSet(true);
/*     */           } else {
/* 478 */             TProtocolUtil.skip(iprot, schemeField.type);
/*     */           }
/* 480 */           break;
/*     */         case 2:
/* 482 */           if (schemeField.type == 11) {
/* 483 */             struct.id = iprot.readString();
/* 484 */             struct.setIdIsSet(true);
/*     */           } else {
/* 486 */             TProtocolUtil.skip(iprot, schemeField.type);
/*     */           }
/* 488 */           break;
/*     */         case 3:
/* 490 */           if (schemeField.type == 11) {
/* 491 */             struct.tenantId = iprot.readString();
/* 492 */             struct.setTenantIdIsSet(true);
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
/*     */     public void write(TProtocol oprot, Token struct) throws TException {
/* 509 */       struct.validate();
/*     */ 
/* 511 */       oprot.writeStructBegin(Token.STRUCT_DESC);
/* 512 */       if (struct.expires != null) {
/* 513 */         oprot.writeFieldBegin(Token.EXPIRES_FIELD_DESC);
/* 514 */         oprot.writeString(struct.expires);
/* 515 */         oprot.writeFieldEnd();
/*     */       }
/* 517 */       if (struct.id != null) {
/* 518 */         oprot.writeFieldBegin(Token.ID_FIELD_DESC);
/* 519 */         oprot.writeString(struct.id);
/* 520 */         oprot.writeFieldEnd();
/*     */       }
/* 522 */       if ((struct.tenantId != null) && 
/* 523 */         (struct.isSetTenantId())) {
/* 524 */         oprot.writeFieldBegin(Token.TENANT_ID_FIELD_DESC);
/* 525 */         oprot.writeString(struct.tenantId);
/* 526 */         oprot.writeFieldEnd();
/*     */       }
/*     */ 
/* 529 */       oprot.writeFieldStop();
/* 530 */       oprot.writeStructEnd();
/*     */     }
/*     */   }
/*     */ 
/*     */   private static class TokenStandardSchemeFactory
/*     */     implements SchemeFactory
/*     */   {
/*     */     public Token.TokenStandardScheme getScheme()
/*     */     {
/* 457 */       return new Token.TokenStandardScheme();
/*     */     }
/*     */   }
/*     */ 
/*     */   public static enum _Fields
/*     */     implements TFieldIdEnum
/*     */   {
/*  52 */     EXPIRES((short)1, "expires"), 
/*  53 */     ID((short)2, "id"), 
/*  54 */     TENANT_ID((short)3, "tenantId");
/*     */ 
/*     */     private static final Map<String, _Fields> byName;
/*     */     private final short _thriftId;
/*     */     private final String _fieldName;
/*     */ 
/*     */     public static _Fields findByThriftId(int fieldId)
/*     */     {
/*  68 */       switch (fieldId) {
/*     */       case 1:
/*  70 */         return EXPIRES;
/*     */       case 2:
/*  72 */         return ID;
/*     */       case 3:
/*  74 */         return TENANT_ID;
/*     */       }
/*  76 */       return null;
/*     */     }
/*     */ 
/*     */     public static _Fields findByThriftIdOrThrow(int fieldId)
/*     */     {
/*  85 */       _Fields fields = findByThriftId(fieldId);
/*  86 */       if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
/*  87 */       return fields;
/*     */     }
/*     */ 
/*     */     public static _Fields findByName(String name)
/*     */     {
/*  94 */       return (_Fields)byName.get(name);
/*     */     }
/*     */ 
/*     */     private _Fields(short thriftId, String fieldName)
/*     */     {
/* 101 */       this._thriftId = thriftId;
/* 102 */       this._fieldName = fieldName;
/*     */     }
/*     */ 
/*     */     public short getThriftFieldId() {
/* 106 */       return this._thriftId;
/*     */     }
/*     */ 
/*     */     public String getFieldName() {
/* 110 */       return this._fieldName;
/*     */     }
/*     */ 
/*     */     static
/*     */     {
/*  56 */       byName = new HashMap();
/*     */ 
/*  59 */       for (_Fields field : EnumSet.allOf(_Fields.class))
/*  60 */         byName.put(field.getFieldName(), field);
/*     */     }
/*     */   }
/*     */ }

/* Location:           /Users/johnderr/.m2/repository/com/hp/csbu/cc/CsThriftModel/1.2-SNAPSHOT/CsThriftModel-1.2-20140130.160951-1223.jar
 * Qualified Name:     com.hp.csbu.cc.security.cs.thrift.service.Token
 * JD-Core Version:    0.6.2
 */