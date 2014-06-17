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
/*     */ import org.apache.thrift.meta_data.StructMetaData;
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
/*     */ public class AuthResponse
/*     */   implements TBase<AuthResponse, AuthResponse._Fields>, Serializable, Cloneable
/*     */ {
/*  35 */   private static final TStruct STRUCT_DESC = new TStruct("AuthResponse");
/*     */ 
/*  37 */   private static final TField TOKEN_INFO_FIELD_DESC = new TField("tokenInfo", (byte)12, (short)1);
/*  38 */   private static final TField USER_INFO_FIELD_DESC = new TField("userInfo", (byte)12, (short)2);
/*     */ 
/*  40 */   private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap();
/*     */   public Token tokenInfo;
/*     */   public User userInfo;
/*     */   public static final Map<_Fields, FieldMetaData> metaDataMap;
/*     */ 
/*     */   public AuthResponse()
/*     */   {
/*     */   }
/*     */ 
/*     */   public AuthResponse(Token tokenInfo, User userInfo)
/*     */   {
/* 129 */     this();
/* 130 */     this.tokenInfo = tokenInfo;
/* 131 */     this.userInfo = userInfo;
/*     */   }
/*     */ 
/*     */   public AuthResponse(AuthResponse other)
/*     */   {
/* 138 */     if (other.isSetTokenInfo()) {
/* 139 */       this.tokenInfo = new Token(other.tokenInfo);
/*     */     }
/* 141 */     if (other.isSetUserInfo())
/* 142 */       this.userInfo = new User(other.userInfo);
/*     */   }
/*     */ 
/*     */   public AuthResponse deepCopy()
/*     */   {
/* 147 */     return new AuthResponse(this);
/*     */   }
/*     */ 
/*     */   public void clear()
/*     */   {
/* 152 */     this.tokenInfo = null;
/* 153 */     this.userInfo = null;
/*     */   }
/*     */ 
/*     */   public Token getTokenInfo() {
/* 157 */     return this.tokenInfo;
/*     */   }
/*     */ 
/*     */   public AuthResponse setTokenInfo(Token tokenInfo) {
/* 161 */     this.tokenInfo = tokenInfo;
/* 162 */     return this;
/*     */   }
/*     */ 
/*     */   public void unsetTokenInfo() {
/* 166 */     this.tokenInfo = null;
/*     */   }
/*     */ 
/*     */   public boolean isSetTokenInfo()
/*     */   {
/* 171 */     return this.tokenInfo != null;
/*     */   }
/*     */ 
/*     */   public void setTokenInfoIsSet(boolean value) {
/* 175 */     if (!value)
/* 176 */       this.tokenInfo = null;
/*     */   }
/*     */ 
/*     */   public User getUserInfo()
/*     */   {
/* 181 */     return this.userInfo;
/*     */   }
/*     */ 
/*     */   public AuthResponse setUserInfo(User userInfo) {
/* 185 */     this.userInfo = userInfo;
/* 186 */     return this;
/*     */   }
/*     */ 
/*     */   public void unsetUserInfo() {
/* 190 */     this.userInfo = null;
/*     */   }
/*     */ 
/*     */   public boolean isSetUserInfo()
/*     */   {
/* 195 */     return this.userInfo != null;
/*     */   }
/*     */ 
/*     */   public void setUserInfoIsSet(boolean value) {
/* 199 */     if (!value)
/* 200 */       this.userInfo = null;
/*     */   }
/*     */ 
/*     */   public void setFieldValue(_Fields field, Object value)
/*     */   {
/* 205 */     switch (field.ordinal()) {//1.$SwitchMap$com$hp$csbu$cc$security$cs$thrift$service$AuthResponse$_Fields[field.ordinal()]) {
/*     */     case 1:
/* 207 */       if (value == null)
/* 208 */         unsetTokenInfo();
/*     */       else {
/* 210 */         setTokenInfo((Token)value);
/*     */       }
/* 212 */       break;
/*     */     case 2:
/* 215 */       if (value == null)
/* 216 */         unsetUserInfo();
/*     */       else
/* 218 */         setUserInfo((User)value);
/*     */       break;
/*     */     }
/*     */   }
/*     */ 
/*     */   public Object getFieldValue(_Fields field)
/*     */   {
/* 226 */     switch (field.ordinal()) {//1.$SwitchMap$com$hp$csbu$cc$security$cs$thrift$service$AuthResponse$_Fields[field.ordinal()]) {
/*     */     case 1:
/* 228 */       return getTokenInfo();
/*     */     case 2:
/* 231 */       return getUserInfo();
/*     */     }
/*     */ 
/* 234 */     throw new IllegalStateException();
/*     */   }
/*     */ 
/*     */   public boolean isSet(_Fields field)
/*     */   {
/* 239 */     if (field == null) {
/* 240 */       throw new IllegalArgumentException();
/*     */     }
/*     */ 
/* 243 */     switch (field.ordinal()) {//1.$SwitchMap$com$hp$csbu$cc$security$cs$thrift$service$AuthResponse$_Fields[field.ordinal()]) {
/*     */     case 1:
/* 245 */       return isSetTokenInfo();
/*     */     case 2:
/* 247 */       return isSetUserInfo();
/*     */     }
/* 249 */     throw new IllegalStateException();
/*     */   }
/*     */ 
/*     */   public boolean equals(Object that)
/*     */   {
/* 254 */     if (that == null)
/* 255 */       return false;
/* 256 */     if ((that instanceof AuthResponse))
/* 257 */       return equals((AuthResponse)that);
/* 258 */     return false;
/*     */   }
/*     */ 
/*     */   public boolean equals(AuthResponse that) {
/* 262 */     if (that == null) {
/* 263 */       return false;
/*     */     }
/* 265 */     boolean this_present_tokenInfo = isSetTokenInfo();
/* 266 */     boolean that_present_tokenInfo = that.isSetTokenInfo();
/* 267 */     if ((this_present_tokenInfo) || (that_present_tokenInfo)) {
/* 268 */       if ((!this_present_tokenInfo) || (!that_present_tokenInfo))
/* 269 */         return false;
/* 270 */       if (!this.tokenInfo.equals(that.tokenInfo)) {
/* 271 */         return false;
/*     */       }
/*     */     }
/* 274 */     boolean this_present_userInfo = isSetUserInfo();
/* 275 */     boolean that_present_userInfo = that.isSetUserInfo();
/* 276 */     if ((this_present_userInfo) || (that_present_userInfo)) {
/* 277 */       if ((!this_present_userInfo) || (!that_present_userInfo))
/* 278 */         return false;
/* 279 */       if (!this.userInfo.equals(that.userInfo)) {
/* 280 */         return false;
/*     */       }
/*     */     }
/* 283 */     return true;
/*     */   }
/*     */ 
/*     */   public int hashCode()
/*     */   {
/* 288 */     return 0;
/*     */   }
/*     */ 
/*     */   public int compareTo(AuthResponse other) {
/* 292 */     if (!getClass().equals(other.getClass())) {
/* 293 */       return getClass().getName().compareTo(other.getClass().getName());
/*     */     }
/*     */ 
/* 296 */     int lastComparison = 0;
/* 297 */     AuthResponse typedOther = other;
/*     */ 
/* 299 */     lastComparison = Boolean.valueOf(isSetTokenInfo()).compareTo(Boolean.valueOf(typedOther.isSetTokenInfo()));
/* 300 */     if (lastComparison != 0) {
/* 301 */       return lastComparison;
/*     */     }
/* 303 */     if (isSetTokenInfo()) {
/* 304 */       lastComparison = TBaseHelper.compareTo(this.tokenInfo, typedOther.tokenInfo);
/* 305 */       if (lastComparison != 0) {
/* 306 */         return lastComparison;
/*     */       }
/*     */     }
/* 309 */     lastComparison = Boolean.valueOf(isSetUserInfo()).compareTo(Boolean.valueOf(typedOther.isSetUserInfo()));
/* 310 */     if (lastComparison != 0) {
/* 311 */       return lastComparison;
/*     */     }
/* 313 */     if (isSetUserInfo()) {
/* 314 */       lastComparison = TBaseHelper.compareTo(this.userInfo, typedOther.userInfo);
/* 315 */       if (lastComparison != 0) {
/* 316 */         return lastComparison;
/*     */       }
/*     */     }
/* 319 */     return 0;
/*     */   }
/*     */ 
/*     */   public _Fields fieldForId(int fieldId) {
/* 323 */     return _Fields.findByThriftId(fieldId);
/*     */   }
/*     */ 
/*     */   public void read(TProtocol iprot) throws TException {
/* 327 */     ((SchemeFactory)schemes.get(iprot.getScheme())).getScheme().read(iprot, this);
/*     */   }
/*     */ 
/*     */   public void write(TProtocol oprot) throws TException {
/* 331 */     ((SchemeFactory)schemes.get(oprot.getScheme())).getScheme().write(oprot, this);
/*     */   }
/*     */ 
/*     */   public String toString()
/*     */   {
/* 336 */     StringBuilder sb = new StringBuilder("AuthResponse(");
/* 337 */     boolean first = true;
/*     */ 
/* 339 */     sb.append("tokenInfo:");
/* 340 */     if (this.tokenInfo == null)
/* 341 */       sb.append("null");
/*     */     else {
/* 343 */       sb.append(this.tokenInfo);
/*     */     }
/* 345 */     first = false;
/* 346 */     if (!first) sb.append(", ");
/* 347 */     sb.append("userInfo:");
/* 348 */     if (this.userInfo == null)
/* 349 */       sb.append("null");
/*     */     else {
/* 351 */       sb.append(this.userInfo);
/*     */     }
/* 353 */     first = false;
/* 354 */     sb.append(")");
/* 355 */     return sb.toString();
/*     */   }
/*     */ 
/*     */   public void validate() throws TException
/*     */   {
/*     */   }
/*     */ 
/*     */   private void writeObject(ObjectOutputStream out) throws IOException {
/*     */     try {
/* 364 */       write(new TCompactProtocol(new TIOStreamTransport(out)));
/*     */     } catch (TException te) {
/* 366 */       throw new IOException(te);
/*     */     }
/*     */   }
/*     */ 
/*     */   private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
/*     */     try {
/* 372 */       read(new TCompactProtocol(new TIOStreamTransport(in)));
/*     */     } catch (TException te) {
/* 374 */       throw new IOException(te);
/*     */     }
/*     */   }
/*     */ 
/*     */   static
/*     */   {
/*  42 */     schemes.put(StandardScheme.class, new AuthResponseStandardSchemeFactory());
/*  43 */     schemes.put(TupleScheme.class, new AuthResponseTupleSchemeFactory());
/*     */ 
/* 113 */     Map tmpMap = new EnumMap(_Fields.class);
/* 114 */     tmpMap.put(_Fields.TOKEN_INFO, new FieldMetaData("tokenInfo", (byte)3, new StructMetaData((byte)12, Token.class)));
/*     */ 
/* 116 */     tmpMap.put(_Fields.USER_INFO, new FieldMetaData("userInfo", (byte)3, new StructMetaData((byte)12, User.class)));
/*     */ 
/* 118 */     metaDataMap = Collections.unmodifiableMap(tmpMap);
/* 119 */     FieldMetaData.addStructMetaDataMap(AuthResponse.class, metaDataMap);
/*     */   }
/*     */ 
/*     */   private static class AuthResponseTupleScheme extends TupleScheme<AuthResponse>
/*     */   {
/*     */     public void write(TProtocol prot, AuthResponse struct)
/*     */       throws TException
/*     */     {
/* 455 */       TTupleProtocol oprot = (TTupleProtocol)prot;
/* 456 */       BitSet optionals = new BitSet();
/* 457 */       if (struct.isSetTokenInfo()) {
/* 458 */         optionals.set(0);
/*     */       }
/* 460 */       if (struct.isSetUserInfo()) {
/* 461 */         optionals.set(1);
/*     */       }
/* 463 */       oprot.writeBitSet(optionals, 2);
/* 464 */       if (struct.isSetTokenInfo()) {
/* 465 */         struct.tokenInfo.write(oprot);
/*     */       }
/* 467 */       if (struct.isSetUserInfo())
/* 468 */         struct.userInfo.write(oprot);
/*     */     }
/*     */ 
/*     */     public void read(TProtocol prot, AuthResponse struct)
/*     */       throws TException
/*     */     {
/* 474 */       TTupleProtocol iprot = (TTupleProtocol)prot;
/* 475 */       BitSet incoming = iprot.readBitSet(2);
/* 476 */       if (incoming.get(0)) {
/* 477 */         struct.tokenInfo = new Token();
/* 478 */         struct.tokenInfo.read(iprot);
/* 479 */         struct.setTokenInfoIsSet(true);
/*     */       }
/* 481 */       if (incoming.get(1)) {
/* 482 */         struct.userInfo = new User();
/* 483 */         struct.userInfo.read(iprot);
/* 484 */         struct.setUserInfoIsSet(true);
/*     */       }
/*     */     }
/*     */   }
/*     */ 
/*     */   private static class AuthResponseTupleSchemeFactory
/*     */     implements SchemeFactory
/*     */   {
/*     */     public AuthResponse.AuthResponseTupleScheme getScheme()
/*     */     {
/* 447 */       return new AuthResponse.AuthResponseTupleScheme();
/*     */     }
/*     */   }
/*     */ 
/*     */   private static class AuthResponseStandardScheme extends StandardScheme<AuthResponse>
/*     */   {
/*     */     public void read(TProtocol iprot, AuthResponse struct)
/*     */       throws TException
/*     */     {
/* 388 */       iprot.readStructBegin();
/*     */       while (true)
/*     */       {
/* 391 */         TField schemeField = iprot.readFieldBegin();
/* 392 */         if (schemeField.type == 0) {
/*     */           break;
/*     */         }
/* 395 */         switch (schemeField.id) {
/*     */         case 1:
/* 397 */           if (schemeField.type == 12) {
/* 398 */             struct.tokenInfo = new Token();
/* 399 */             struct.tokenInfo.read(iprot);
/* 400 */             struct.setTokenInfoIsSet(true);
/*     */           } else {
/* 402 */             TProtocolUtil.skip(iprot, schemeField.type);
/*     */           }
/* 404 */           break;
/*     */         case 2:
/* 406 */           if (schemeField.type == 12) {
/* 407 */             struct.userInfo = new User();
/* 408 */             struct.userInfo.read(iprot);
/* 409 */             struct.setUserInfoIsSet(true);
/*     */           } else {
/* 411 */             TProtocolUtil.skip(iprot, schemeField.type);
/*     */           }
/* 413 */           break;
/*     */         default:
/* 415 */           TProtocolUtil.skip(iprot, schemeField.type);
/*     */         }
/* 417 */         iprot.readFieldEnd();
/*     */       }
/* 419 */       iprot.readStructEnd();
/*     */ 
/* 422 */       struct.validate();
/*     */     }
/*     */ 
/*     */     public void write(TProtocol oprot, AuthResponse struct) throws TException {
/* 426 */       struct.validate();
/*     */ 
/* 428 */       oprot.writeStructBegin(AuthResponse.STRUCT_DESC);
/* 429 */       if (struct.tokenInfo != null) {
/* 430 */         oprot.writeFieldBegin(AuthResponse.TOKEN_INFO_FIELD_DESC);
/* 431 */         struct.tokenInfo.write(oprot);
/* 432 */         oprot.writeFieldEnd();
/*     */       }
/* 434 */       if (struct.userInfo != null) {
/* 435 */         oprot.writeFieldBegin(AuthResponse.USER_INFO_FIELD_DESC);
/* 436 */         struct.userInfo.write(oprot);
/* 437 */         oprot.writeFieldEnd();
/*     */       }
/* 439 */       oprot.writeFieldStop();
/* 440 */       oprot.writeStructEnd();
/*     */     }
/*     */   }
/*     */ 
/*     */   private static class AuthResponseStandardSchemeFactory
/*     */     implements SchemeFactory
/*     */   {
/*     */     public AuthResponse.AuthResponseStandardScheme getScheme()
/*     */     {
/* 380 */       return new AuthResponse.AuthResponseStandardScheme();
/*     */     }
/*     */   }
/*     */ 
/*     */   public static enum _Fields
/*     */     implements TFieldIdEnum
/*     */   {
/*  51 */     TOKEN_INFO((short)1, "tokenInfo"), 
/*  52 */     USER_INFO((short)2, "userInfo");
/*     */ 
/*     */     private static final Map<String, _Fields> byName;
/*     */     private final short _thriftId;
/*     */     private final String _fieldName;
/*     */ 
/*     */     public static _Fields findByThriftId(int fieldId)
/*     */     {
/*  66 */       switch (fieldId) {
/*     */       case 1:
/*  68 */         return TOKEN_INFO;
/*     */       case 2:
/*  70 */         return USER_INFO;
/*     */       }
/*  72 */       return null;
/*     */     }
/*     */ 
/*     */     public static _Fields findByThriftIdOrThrow(int fieldId)
/*     */     {
/*  81 */       _Fields fields = findByThriftId(fieldId);
/*  82 */       if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
/*  83 */       return fields;
/*     */     }
/*     */ 
/*     */     public static _Fields findByName(String name)
/*     */     {
/*  90 */       return (_Fields)byName.get(name);
/*     */     }
/*     */ 
/*     */     private _Fields(short thriftId, String fieldName)
/*     */     {
/*  97 */       this._thriftId = thriftId;
/*  98 */       this._fieldName = fieldName;
/*     */     }
/*     */ 
/*     */     public short getThriftFieldId() {
/* 102 */       return this._thriftId;
/*     */     }
/*     */ 
/*     */     public String getFieldName() {
/* 106 */       return this._fieldName;
/*     */     }
/*     */ 
/*     */     static
/*     */     {
/*  54 */       byName = new HashMap();
/*     */ 
/*  57 */       for (_Fields field : EnumSet.allOf(_Fields.class))
/*  58 */         byName.put(field.getFieldName(), field);
/*     */     }
/*     */   }
/*     */ }

/* Location:           /Users/johnderr/.m2/repository/com/hp/csbu/cc/CsThriftModel/1.2-SNAPSHOT/CsThriftModel-1.2-20140130.160951-1223.jar
 * Qualified Name:     com.hp.csbu.cc.security.cs.thrift.service.AuthResponse
 * JD-Core Version:    0.6.2
 */