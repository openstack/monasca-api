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
/*     */ public class SignatureCredentials
/*     */   implements TBase<SignatureCredentials, SignatureCredentials._Fields>, Serializable, Cloneable
/*     */ {
/*  34 */   private static final TStruct STRUCT_DESC = new TStruct("SignatureCredentials");
/*     */ 
/*  36 */   private static final TField KEY_ID_FIELD_DESC = new TField("keyId", (byte)11, (short)1);
/*  37 */   private static final TField KEY_TYPE_FIELD_DESC = new TField("keyType", (byte)11, (short)2);
/*  38 */   private static final TField SIGNATURE_METHOD_FIELD_DESC = new TField("signatureMethod", (byte)11, (short)3);
/*  39 */   private static final TField DATA_TO_SIGN_FIELD_DESC = new TField("dataToSign", (byte)11, (short)4);
/*  40 */   private static final TField SIGNATURE_FIELD_DESC = new TField("signature", (byte)11, (short)5);
/*     */ 
/*  42 */   private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap();
/*     */   public String keyId;
/*     */   public String keyType;
/*     */   public String signatureMethod;
/*     */   public String dataToSign;
/*     */   public String signature;
/*     */   public static final Map<_Fields, FieldMetaData> metaDataMap;
/*     */ 
/*     */   public SignatureCredentials()
/*     */   {
/*     */   }
/*     */ 
/*     */   public SignatureCredentials(String keyId, String keyType, String signatureMethod, String dataToSign, String signature)
/*     */   {
/* 152 */     this();
/* 153 */     this.keyId = keyId;
/* 154 */     this.keyType = keyType;
/* 155 */     this.signatureMethod = signatureMethod;
/* 156 */     this.dataToSign = dataToSign;
/* 157 */     this.signature = signature;
/*     */   }
/*     */ 
/*     */   public SignatureCredentials(SignatureCredentials other)
/*     */   {
/* 164 */     if (other.isSetKeyId()) {
/* 165 */       this.keyId = other.keyId;
/*     */     }
/* 167 */     if (other.isSetKeyType()) {
/* 168 */       this.keyType = other.keyType;
/*     */     }
/* 170 */     if (other.isSetSignatureMethod()) {
/* 171 */       this.signatureMethod = other.signatureMethod;
/*     */     }
/* 173 */     if (other.isSetDataToSign()) {
/* 174 */       this.dataToSign = other.dataToSign;
/*     */     }
/* 176 */     if (other.isSetSignature())
/* 177 */       this.signature = other.signature;
/*     */   }
/*     */ 
/*     */   public SignatureCredentials deepCopy()
/*     */   {
/* 182 */     return new SignatureCredentials(this);
/*     */   }
/*     */ 
/*     */   public void clear()
/*     */   {
/* 187 */     this.keyId = null;
/* 188 */     this.keyType = null;
/* 189 */     this.signatureMethod = null;
/* 190 */     this.dataToSign = null;
/* 191 */     this.signature = null;
/*     */   }
/*     */ 
/*     */   public String getKeyId() {
/* 195 */     return this.keyId;
/*     */   }
/*     */ 
/*     */   public SignatureCredentials setKeyId(String keyId) {
/* 199 */     this.keyId = keyId;
/* 200 */     return this;
/*     */   }
/*     */ 
/*     */   public void unsetKeyId() {
/* 204 */     this.keyId = null;
/*     */   }
/*     */ 
/*     */   public boolean isSetKeyId()
/*     */   {
/* 209 */     return this.keyId != null;
/*     */   }
/*     */ 
/*     */   public void setKeyIdIsSet(boolean value) {
/* 213 */     if (!value)
/* 214 */       this.keyId = null;
/*     */   }
/*     */ 
/*     */   public String getKeyType()
/*     */   {
/* 219 */     return this.keyType;
/*     */   }
/*     */ 
/*     */   public SignatureCredentials setKeyType(String keyType) {
/* 223 */     this.keyType = keyType;
/* 224 */     return this;
/*     */   }
/*     */ 
/*     */   public void unsetKeyType() {
/* 228 */     this.keyType = null;
/*     */   }
/*     */ 
/*     */   public boolean isSetKeyType()
/*     */   {
/* 233 */     return this.keyType != null;
/*     */   }
/*     */ 
/*     */   public void setKeyTypeIsSet(boolean value) {
/* 237 */     if (!value)
/* 238 */       this.keyType = null;
/*     */   }
/*     */ 
/*     */   public String getSignatureMethod()
/*     */   {
/* 243 */     return this.signatureMethod;
/*     */   }
/*     */ 
/*     */   public SignatureCredentials setSignatureMethod(String signatureMethod) {
/* 247 */     this.signatureMethod = signatureMethod;
/* 248 */     return this;
/*     */   }
/*     */ 
/*     */   public void unsetSignatureMethod() {
/* 252 */     this.signatureMethod = null;
/*     */   }
/*     */ 
/*     */   public boolean isSetSignatureMethod()
/*     */   {
/* 257 */     return this.signatureMethod != null;
/*     */   }
/*     */ 
/*     */   public void setSignatureMethodIsSet(boolean value) {
/* 261 */     if (!value)
/* 262 */       this.signatureMethod = null;
/*     */   }
/*     */ 
/*     */   public String getDataToSign()
/*     */   {
/* 267 */     return this.dataToSign;
/*     */   }
/*     */ 
/*     */   public SignatureCredentials setDataToSign(String dataToSign) {
/* 271 */     this.dataToSign = dataToSign;
/* 272 */     return this;
/*     */   }
/*     */ 
/*     */   public void unsetDataToSign() {
/* 276 */     this.dataToSign = null;
/*     */   }
/*     */ 
/*     */   public boolean isSetDataToSign()
/*     */   {
/* 281 */     return this.dataToSign != null;
/*     */   }
/*     */ 
/*     */   public void setDataToSignIsSet(boolean value) {
/* 285 */     if (!value)
/* 286 */       this.dataToSign = null;
/*     */   }
/*     */ 
/*     */   public String getSignature()
/*     */   {
/* 291 */     return this.signature;
/*     */   }
/*     */ 
/*     */   public SignatureCredentials setSignature(String signature) {
/* 295 */     this.signature = signature;
/* 296 */     return this;
/*     */   }
/*     */ 
/*     */   public void unsetSignature() {
/* 300 */     this.signature = null;
/*     */   }
/*     */ 
/*     */   public boolean isSetSignature()
/*     */   {
/* 305 */     return this.signature != null;
/*     */   }
/*     */ 
/*     */   public void setSignatureIsSet(boolean value) {
/* 309 */     if (!value)
/* 310 */       this.signature = null;
/*     */   }
/*     */ 
/*     */   public void setFieldValue(_Fields field, Object value)
/*     */   {
/* 315 */     switch (field.ordinal()) {//1.$SwitchMap$com$hp$csbu$cc$security$cs$thrift$service$SignatureCredentials$_Fields[field.ordinal()]) {
/*     */     case 1:
/* 317 */       if (value == null)
/* 318 */         unsetKeyId();
/*     */       else {
/* 320 */         setKeyId((String)value);
/*     */       }
/* 322 */       break;
/*     */     case 2:
/* 325 */       if (value == null)
/* 326 */         unsetKeyType();
/*     */       else {
/* 328 */         setKeyType((String)value);
/*     */       }
/* 330 */       break;
/*     */     case 3:
/* 333 */       if (value == null)
/* 334 */         unsetSignatureMethod();
/*     */       else {
/* 336 */         setSignatureMethod((String)value);
/*     */       }
/* 338 */       break;
/*     */     case 4:
/* 341 */       if (value == null)
/* 342 */         unsetDataToSign();
/*     */       else {
/* 344 */         setDataToSign((String)value);
/*     */       }
/* 346 */       break;
/*     */     case 5:
/* 349 */       if (value == null)
/* 350 */         unsetSignature();
/*     */       else
/* 352 */         setSignature((String)value);
/*     */       break;
/*     */     }
/*     */   }
/*     */ 
/*     */   public Object getFieldValue(_Fields field)
/*     */   {
/* 360 */     switch (field.ordinal()) {//1.$SwitchMap$com$hp$csbu$cc$security$cs$thrift$service$SignatureCredentials$_Fields[field.ordinal()]) {
/*     */     case 1:
/* 362 */       return getKeyId();
/*     */     case 2:
/* 365 */       return getKeyType();
/*     */     case 3:
/* 368 */       return getSignatureMethod();
/*     */     case 4:
/* 371 */       return getDataToSign();
/*     */     case 5:
/* 374 */       return getSignature();
/*     */     }
/*     */ 
/* 377 */     throw new IllegalStateException();
/*     */   }
/*     */ 
/*     */   public boolean isSet(_Fields field)
/*     */   {
/* 382 */     if (field == null) {
/* 383 */       throw new IllegalArgumentException();
/*     */     }
/*     */ 
/* 386 */     switch (field.ordinal()) { //1.$SwitchMap$com$hp$csbu$cc$security$cs$thrift$service$SignatureCredentials$_Fields[field.ordinal()]) {
/*     */     case 1:
/* 388 */       return isSetKeyId();
/*     */     case 2:
/* 390 */       return isSetKeyType();
/*     */     case 3:
/* 392 */       return isSetSignatureMethod();
/*     */     case 4:
/* 394 */       return isSetDataToSign();
/*     */     case 5:
/* 396 */       return isSetSignature();
/*     */     }
/* 398 */     throw new IllegalStateException();
/*     */   }
/*     */ 
/*     */   public boolean equals(Object that)
/*     */   {
/* 403 */     if (that == null)
/* 404 */       return false;
/* 405 */     if ((that instanceof SignatureCredentials))
/* 406 */       return equals((SignatureCredentials)that);
/* 407 */     return false;
/*     */   }
/*     */ 
/*     */   public boolean equals(SignatureCredentials that) {
/* 411 */     if (that == null) {
/* 412 */       return false;
/*     */     }
/* 414 */     boolean this_present_keyId = isSetKeyId();
/* 415 */     boolean that_present_keyId = that.isSetKeyId();
/* 416 */     if ((this_present_keyId) || (that_present_keyId)) {
/* 417 */       if ((!this_present_keyId) || (!that_present_keyId))
/* 418 */         return false;
/* 419 */       if (!this.keyId.equals(that.keyId)) {
/* 420 */         return false;
/*     */       }
/*     */     }
/* 423 */     boolean this_present_keyType = isSetKeyType();
/* 424 */     boolean that_present_keyType = that.isSetKeyType();
/* 425 */     if ((this_present_keyType) || (that_present_keyType)) {
/* 426 */       if ((!this_present_keyType) || (!that_present_keyType))
/* 427 */         return false;
/* 428 */       if (!this.keyType.equals(that.keyType)) {
/* 429 */         return false;
/*     */       }
/*     */     }
/* 432 */     boolean this_present_signatureMethod = isSetSignatureMethod();
/* 433 */     boolean that_present_signatureMethod = that.isSetSignatureMethod();
/* 434 */     if ((this_present_signatureMethod) || (that_present_signatureMethod)) {
/* 435 */       if ((!this_present_signatureMethod) || (!that_present_signatureMethod))
/* 436 */         return false;
/* 437 */       if (!this.signatureMethod.equals(that.signatureMethod)) {
/* 438 */         return false;
/*     */       }
/*     */     }
/* 441 */     boolean this_present_dataToSign = isSetDataToSign();
/* 442 */     boolean that_present_dataToSign = that.isSetDataToSign();
/* 443 */     if ((this_present_dataToSign) || (that_present_dataToSign)) {
/* 444 */       if ((!this_present_dataToSign) || (!that_present_dataToSign))
/* 445 */         return false;
/* 446 */       if (!this.dataToSign.equals(that.dataToSign)) {
/* 447 */         return false;
/*     */       }
/*     */     }
/* 450 */     boolean this_present_signature = isSetSignature();
/* 451 */     boolean that_present_signature = that.isSetSignature();
/* 452 */     if ((this_present_signature) || (that_present_signature)) {
/* 453 */       if ((!this_present_signature) || (!that_present_signature))
/* 454 */         return false;
/* 455 */       if (!this.signature.equals(that.signature)) {
/* 456 */         return false;
/*     */       }
/*     */     }
/* 459 */     return true;
/*     */   }
/*     */ 
/*     */   public int hashCode()
/*     */   {
/* 464 */     return 0;
/*     */   }
/*     */ 
/*     */   public int compareTo(SignatureCredentials other) {
/* 468 */     if (!getClass().equals(other.getClass())) {
/* 469 */       return getClass().getName().compareTo(other.getClass().getName());
/*     */     }
/*     */ 
/* 472 */     int lastComparison = 0;
/* 473 */     SignatureCredentials typedOther = other;
/*     */ 
/* 475 */     lastComparison = Boolean.valueOf(isSetKeyId()).compareTo(Boolean.valueOf(typedOther.isSetKeyId()));
/* 476 */     if (lastComparison != 0) {
/* 477 */       return lastComparison;
/*     */     }
/* 479 */     if (isSetKeyId()) {
/* 480 */       lastComparison = TBaseHelper.compareTo(this.keyId, typedOther.keyId);
/* 481 */       if (lastComparison != 0) {
/* 482 */         return lastComparison;
/*     */       }
/*     */     }
/* 485 */     lastComparison = Boolean.valueOf(isSetKeyType()).compareTo(Boolean.valueOf(typedOther.isSetKeyType()));
/* 486 */     if (lastComparison != 0) {
/* 487 */       return lastComparison;
/*     */     }
/* 489 */     if (isSetKeyType()) {
/* 490 */       lastComparison = TBaseHelper.compareTo(this.keyType, typedOther.keyType);
/* 491 */       if (lastComparison != 0) {
/* 492 */         return lastComparison;
/*     */       }
/*     */     }
/* 495 */     lastComparison = Boolean.valueOf(isSetSignatureMethod()).compareTo(Boolean.valueOf(typedOther.isSetSignatureMethod()));
/* 496 */     if (lastComparison != 0) {
/* 497 */       return lastComparison;
/*     */     }
/* 499 */     if (isSetSignatureMethod()) {
/* 500 */       lastComparison = TBaseHelper.compareTo(this.signatureMethod, typedOther.signatureMethod);
/* 501 */       if (lastComparison != 0) {
/* 502 */         return lastComparison;
/*     */       }
/*     */     }
/* 505 */     lastComparison = Boolean.valueOf(isSetDataToSign()).compareTo(Boolean.valueOf(typedOther.isSetDataToSign()));
/* 506 */     if (lastComparison != 0) {
/* 507 */       return lastComparison;
/*     */     }
/* 509 */     if (isSetDataToSign()) {
/* 510 */       lastComparison = TBaseHelper.compareTo(this.dataToSign, typedOther.dataToSign);
/* 511 */       if (lastComparison != 0) {
/* 512 */         return lastComparison;
/*     */       }
/*     */     }
/* 515 */     lastComparison = Boolean.valueOf(isSetSignature()).compareTo(Boolean.valueOf(typedOther.isSetSignature()));
/* 516 */     if (lastComparison != 0) {
/* 517 */       return lastComparison;
/*     */     }
/* 519 */     if (isSetSignature()) {
/* 520 */       lastComparison = TBaseHelper.compareTo(this.signature, typedOther.signature);
/* 521 */       if (lastComparison != 0) {
/* 522 */         return lastComparison;
/*     */       }
/*     */     }
/* 525 */     return 0;
/*     */   }
/*     */ 
/*     */   public _Fields fieldForId(int fieldId) {
/* 529 */     return _Fields.findByThriftId(fieldId);
/*     */   }
/*     */ 
/*     */   public void read(TProtocol iprot) throws TException {
/* 533 */     ((SchemeFactory)schemes.get(iprot.getScheme())).getScheme().read(iprot, this);
/*     */   }
/*     */ 
/*     */   public void write(TProtocol oprot) throws TException {
/* 537 */     ((SchemeFactory)schemes.get(oprot.getScheme())).getScheme().write(oprot, this);
/*     */   }
/*     */ 
/*     */   public String toString()
/*     */   {
/* 542 */     StringBuilder sb = new StringBuilder("SignatureCredentials(");
/* 543 */     boolean first = true;
/*     */ 
/* 545 */     sb.append("keyId:");
/* 546 */     if (this.keyId == null)
/* 547 */       sb.append("null");
/*     */     else {
/* 549 */       sb.append(this.keyId);
/*     */     }
/* 551 */     first = false;
/* 552 */     if (!first) sb.append(", ");
/* 553 */     sb.append("keyType:");
/* 554 */     if (this.keyType == null)
/* 555 */       sb.append("null");
/*     */     else {
/* 557 */       sb.append(this.keyType);
/*     */     }
/* 559 */     first = false;
/* 560 */     if (!first) sb.append(", ");
/* 561 */     sb.append("signatureMethod:");
/* 562 */     if (this.signatureMethod == null)
/* 563 */       sb.append("null");
/*     */     else {
/* 565 */       sb.append(this.signatureMethod);
/*     */     }
/* 567 */     first = false;
/* 568 */     if (!first) sb.append(", ");
/* 569 */     sb.append("dataToSign:");
/* 570 */     if (this.dataToSign == null)
/* 571 */       sb.append("null");
/*     */     else {
/* 573 */       sb.append(this.dataToSign);
/*     */     }
/* 575 */     first = false;
/* 576 */     if (!first) sb.append(", ");
/* 577 */     sb.append("signature:");
/* 578 */     if (this.signature == null)
/* 579 */       sb.append("null");
/*     */     else {
/* 581 */       sb.append(this.signature);
/*     */     }
/* 583 */     first = false;
/* 584 */     sb.append(")");
/* 585 */     return sb.toString();
/*     */   }
/*     */ 
/*     */   public void validate() throws TException
/*     */   {
/*     */   }
/*     */ 
/*     */   private void writeObject(ObjectOutputStream out) throws IOException {
/*     */     try {
/* 594 */       write(new TCompactProtocol(new TIOStreamTransport(out)));
/*     */     } catch (TException te) {
/* 596 */       throw new IOException(te);
/*     */     }
/*     */   }
/*     */ 
/*     */   private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
/*     */     try {
/* 602 */       read(new TCompactProtocol(new TIOStreamTransport(in)));
/*     */     } catch (TException te) {
/* 604 */       throw new IOException(te);
/*     */     }
/*     */   }
/*     */ 
/*     */   static
/*     */   {
/*  44 */     schemes.put(StandardScheme.class, new SignatureCredentialsStandardSchemeFactory());
/*  45 */     schemes.put(TupleScheme.class, new SignatureCredentialsTupleSchemeFactory());
/*     */ 
/* 127 */     Map tmpMap = new EnumMap(_Fields.class);
/* 128 */     tmpMap.put(_Fields.KEY_ID, new FieldMetaData("keyId", (byte)3, new FieldValueMetaData((byte)11)));
/*     */ 
/* 130 */     tmpMap.put(_Fields.KEY_TYPE, new FieldMetaData("keyType", (byte)3, new FieldValueMetaData((byte)11)));
/*     */ 
/* 132 */     tmpMap.put(_Fields.SIGNATURE_METHOD, new FieldMetaData("signatureMethod", (byte)3, new FieldValueMetaData((byte)11)));
/*     */ 
/* 134 */     tmpMap.put(_Fields.DATA_TO_SIGN, new FieldMetaData("dataToSign", (byte)3, new FieldValueMetaData((byte)11)));
/*     */ 
/* 136 */     tmpMap.put(_Fields.SIGNATURE, new FieldMetaData("signature", (byte)3, new FieldValueMetaData((byte)11)));
/*     */ 
/* 138 */     metaDataMap = Collections.unmodifiableMap(tmpMap);
/* 139 */     FieldMetaData.addStructMetaDataMap(SignatureCredentials.class, metaDataMap);
/*     */   }
/*     */ 
/*     */   private static class SignatureCredentialsTupleScheme extends TupleScheme<SignatureCredentials>
/*     */   {
/*     */     public void write(TProtocol prot, SignatureCredentials struct)
/*     */       throws TException
/*     */     {
/* 722 */       TTupleProtocol oprot = (TTupleProtocol)prot;
/* 723 */       BitSet optionals = new BitSet();
/* 724 */       if (struct.isSetKeyId()) {
/* 725 */         optionals.set(0);
/*     */       }
/* 727 */       if (struct.isSetKeyType()) {
/* 728 */         optionals.set(1);
/*     */       }
/* 730 */       if (struct.isSetSignatureMethod()) {
/* 731 */         optionals.set(2);
/*     */       }
/* 733 */       if (struct.isSetDataToSign()) {
/* 734 */         optionals.set(3);
/*     */       }
/* 736 */       if (struct.isSetSignature()) {
/* 737 */         optionals.set(4);
/*     */       }
/* 739 */       oprot.writeBitSet(optionals, 5);
/* 740 */       if (struct.isSetKeyId()) {
/* 741 */         oprot.writeString(struct.keyId);
/*     */       }
/* 743 */       if (struct.isSetKeyType()) {
/* 744 */         oprot.writeString(struct.keyType);
/*     */       }
/* 746 */       if (struct.isSetSignatureMethod()) {
/* 747 */         oprot.writeString(struct.signatureMethod);
/*     */       }
/* 749 */       if (struct.isSetDataToSign()) {
/* 750 */         oprot.writeString(struct.dataToSign);
/*     */       }
/* 752 */       if (struct.isSetSignature())
/* 753 */         oprot.writeString(struct.signature);
/*     */     }
/*     */ 
/*     */     public void read(TProtocol prot, SignatureCredentials struct)
/*     */       throws TException
/*     */     {
/* 759 */       TTupleProtocol iprot = (TTupleProtocol)prot;
/* 760 */       BitSet incoming = iprot.readBitSet(5);
/* 761 */       if (incoming.get(0)) {
/* 762 */         struct.keyId = iprot.readString();
/* 763 */         struct.setKeyIdIsSet(true);
/*     */       }
/* 765 */       if (incoming.get(1)) {
/* 766 */         struct.keyType = iprot.readString();
/* 767 */         struct.setKeyTypeIsSet(true);
/*     */       }
/* 769 */       if (incoming.get(2)) {
/* 770 */         struct.signatureMethod = iprot.readString();
/* 771 */         struct.setSignatureMethodIsSet(true);
/*     */       }
/* 773 */       if (incoming.get(3)) {
/* 774 */         struct.dataToSign = iprot.readString();
/* 775 */         struct.setDataToSignIsSet(true);
/*     */       }
/* 777 */       if (incoming.get(4)) {
/* 778 */         struct.signature = iprot.readString();
/* 779 */         struct.setSignatureIsSet(true);
/*     */       }
/*     */     }
/*     */   }
/*     */ 
/*     */   private static class SignatureCredentialsTupleSchemeFactory
/*     */     implements SchemeFactory
/*     */   {
/*     */     public SignatureCredentials.SignatureCredentialsTupleScheme getScheme()
/*     */     {
/* 714 */       return new SignatureCredentials.SignatureCredentialsTupleScheme();
/*     */     }
/*     */   }
/*     */ 
/*     */   private static class SignatureCredentialsStandardScheme extends StandardScheme<SignatureCredentials>
/*     */   {
/*     */     public void read(TProtocol iprot, SignatureCredentials struct)
/*     */       throws TException
/*     */     {
/* 618 */       iprot.readStructBegin();
/*     */       while (true)
/*     */       {
/* 621 */         TField schemeField = iprot.readFieldBegin();
/* 622 */         if (schemeField.type == 0) {
/*     */           break;
/*     */         }
/* 625 */         switch (schemeField.id) {
/*     */         case 1:
/* 627 */           if (schemeField.type == 11) {
/* 628 */             struct.keyId = iprot.readString();
/* 629 */             struct.setKeyIdIsSet(true);
/*     */           } else {
/* 631 */             TProtocolUtil.skip(iprot, schemeField.type);
/*     */           }
/* 633 */           break;
/*     */         case 2:
/* 635 */           if (schemeField.type == 11) {
/* 636 */             struct.keyType = iprot.readString();
/* 637 */             struct.setKeyTypeIsSet(true);
/*     */           } else {
/* 639 */             TProtocolUtil.skip(iprot, schemeField.type);
/*     */           }
/* 641 */           break;
/*     */         case 3:
/* 643 */           if (schemeField.type == 11) {
/* 644 */             struct.signatureMethod = iprot.readString();
/* 645 */             struct.setSignatureMethodIsSet(true);
/*     */           } else {
/* 647 */             TProtocolUtil.skip(iprot, schemeField.type);
/*     */           }
/* 649 */           break;
/*     */         case 4:
/* 651 */           if (schemeField.type == 11) {
/* 652 */             struct.dataToSign = iprot.readString();
/* 653 */             struct.setDataToSignIsSet(true);
/*     */           } else {
/* 655 */             TProtocolUtil.skip(iprot, schemeField.type);
/*     */           }
/* 657 */           break;
/*     */         case 5:
/* 659 */           if (schemeField.type == 11) {
/* 660 */             struct.signature = iprot.readString();
/* 661 */             struct.setSignatureIsSet(true);
/*     */           } else {
/* 663 */             TProtocolUtil.skip(iprot, schemeField.type);
/*     */           }
/* 665 */           break;
/*     */         default:
/* 667 */           TProtocolUtil.skip(iprot, schemeField.type);
/*     */         }
/* 669 */         iprot.readFieldEnd();
/*     */       }
/* 671 */       iprot.readStructEnd();
/*     */ 
/* 674 */       struct.validate();
/*     */     }
/*     */ 
/*     */     public void write(TProtocol oprot, SignatureCredentials struct) throws TException {
/* 678 */       struct.validate();
/*     */ 
/* 680 */       oprot.writeStructBegin(SignatureCredentials.STRUCT_DESC);
/* 681 */       if (struct.keyId != null) {
/* 682 */         oprot.writeFieldBegin(SignatureCredentials.KEY_ID_FIELD_DESC);
/* 683 */         oprot.writeString(struct.keyId);
/* 684 */         oprot.writeFieldEnd();
/*     */       }
/* 686 */       if (struct.keyType != null) {
/* 687 */         oprot.writeFieldBegin(SignatureCredentials.KEY_TYPE_FIELD_DESC);
/* 688 */         oprot.writeString(struct.keyType);
/* 689 */         oprot.writeFieldEnd();
/*     */       }
/* 691 */       if (struct.signatureMethod != null) {
/* 692 */         oprot.writeFieldBegin(SignatureCredentials.SIGNATURE_METHOD_FIELD_DESC);
/* 693 */         oprot.writeString(struct.signatureMethod);
/* 694 */         oprot.writeFieldEnd();
/*     */       }
/* 696 */       if (struct.dataToSign != null) {
/* 697 */         oprot.writeFieldBegin(SignatureCredentials.DATA_TO_SIGN_FIELD_DESC);
/* 698 */         oprot.writeString(struct.dataToSign);
/* 699 */         oprot.writeFieldEnd();
/*     */       }
/* 701 */       if (struct.signature != null) {
/* 702 */         oprot.writeFieldBegin(SignatureCredentials.SIGNATURE_FIELD_DESC);
/* 703 */         oprot.writeString(struct.signature);
/* 704 */         oprot.writeFieldEnd();
/*     */       }
/* 706 */       oprot.writeFieldStop();
/* 707 */       oprot.writeStructEnd();
/*     */     }
/*     */   }
/*     */ 
/*     */   private static class SignatureCredentialsStandardSchemeFactory
/*     */     implements SchemeFactory
/*     */   {
/*     */     public SignatureCredentials.SignatureCredentialsStandardScheme getScheme()
/*     */     {
/* 610 */       return new SignatureCredentials.SignatureCredentialsStandardScheme();
/*     */     }
/*     */   }
/*     */ 
/*     */   public static enum _Fields
/*     */     implements TFieldIdEnum
/*     */   {
/*  56 */     KEY_ID((short)1, "keyId"), 
/*  57 */     KEY_TYPE((short)2, "keyType"), 
/*  58 */     SIGNATURE_METHOD((short)3, "signatureMethod"), 
/*  59 */     DATA_TO_SIGN((short)4, "dataToSign"), 
/*  60 */     SIGNATURE((short)5, "signature");
/*     */ 
/*     */     private static final Map<String, _Fields> byName;
/*     */     private final short _thriftId;
/*     */     private final String _fieldName;
/*     */ 
/*     */     public static _Fields findByThriftId(int fieldId)
/*     */     {
/*  74 */       switch (fieldId) {
/*     */       case 1:
/*  76 */         return KEY_ID;
/*     */       case 2:
/*  78 */         return KEY_TYPE;
/*     */       case 3:
/*  80 */         return SIGNATURE_METHOD;
/*     */       case 4:
/*  82 */         return DATA_TO_SIGN;
/*     */       case 5:
/*  84 */         return SIGNATURE;
/*     */       }
/*  86 */       return null;
/*     */     }
/*     */ 
/*     */     public static _Fields findByThriftIdOrThrow(int fieldId)
/*     */     {
/*  95 */       _Fields fields = findByThriftId(fieldId);
/*  96 */       if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
/*  97 */       return fields;
/*     */     }
/*     */ 
/*     */     public static _Fields findByName(String name)
/*     */     {
/* 104 */       return (_Fields)byName.get(name);
/*     */     }
/*     */ 
/*     */     private _Fields(short thriftId, String fieldName)
/*     */     {
/* 111 */       this._thriftId = thriftId;
/* 112 */       this._fieldName = fieldName;
/*     */     }
/*     */ 
/*     */     public short getThriftFieldId() {
/* 116 */       return this._thriftId;
/*     */     }
/*     */ 
/*     */     public String getFieldName() {
/* 120 */       return this._fieldName;
/*     */     }
/*     */ 
/*     */     static
/*     */     {
/*  62 */       byName = new HashMap();
/*     */ 
/*  65 */       for (_Fields field : EnumSet.allOf(_Fields.class))
/*  66 */         byName.put(field.getFieldName(), field);
/*     */     }
/*     */   }
/*     */ }

/* Location:           /Users/johnderr/.m2/repository/com/hp/csbu/cc/CsThriftModel/1.2-SNAPSHOT/CsThriftModel-1.2-20140130.160951-1223.jar
 * Qualified Name:     com.hp.csbu.cc.security.cs.thrift.service.SignatureCredentials
 * JD-Core Version:    0.6.2
 */