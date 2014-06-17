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
/*     */ public class Role
/*     */   implements TBase<Role, Role._Fields>, Serializable, Cloneable
/*     */ {
/*  34 */   private static final TStruct STRUCT_DESC = new TStruct("Role");
/*     */ 
/*  36 */   private static final TField ID_FIELD_DESC = new TField("id", (byte)11, (short)1);
/*  37 */   private static final TField SERVICE_ID_FIELD_DESC = new TField("serviceId", (byte)11, (short)2);
/*  38 */   private static final TField NAME_FIELD_DESC = new TField("name", (byte)11, (short)3);
/*  39 */   private static final TField TENANT_ID_FIELD_DESC = new TField("tenantId", (byte)11, (short)4);
/*     */ 
/*  41 */   private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap();
/*     */   public String id;
/*     */   public String serviceId;
/*     */   public String name;
/*     */   public String tenantId;
/* 120 */   private _Fields[] optionals = { _Fields.SERVICE_ID, _Fields.NAME, _Fields.TENANT_ID };
/*     */   public static final Map<_Fields, FieldMetaData> metaDataMap;
/*     */ 
/*     */   public Role()
/*     */   {
/*     */   }
/*     */ 
/*     */   public Role(String id)
/*     */   {
/* 142 */     this();
/* 143 */     this.id = id;
/*     */   }
/*     */ 
/*     */   public Role(Role other)
/*     */   {
/* 150 */     if (other.isSetId()) {
/* 151 */       this.id = other.id;
/*     */     }
/* 153 */     if (other.isSetServiceId()) {
/* 154 */       this.serviceId = other.serviceId;
/*     */     }
/* 156 */     if (other.isSetName()) {
/* 157 */       this.name = other.name;
/*     */     }
/* 159 */     if (other.isSetTenantId())
/* 160 */       this.tenantId = other.tenantId;
/*     */   }
/*     */ 
/*     */   public Role deepCopy()
/*     */   {
/* 165 */     return new Role(this);
/*     */   }
/*     */ 
/*     */   public void clear()
/*     */   {
/* 170 */     this.id = null;
/* 171 */     this.serviceId = null;
/* 172 */     this.name = null;
/* 173 */     this.tenantId = null;
/*     */   }
/*     */ 
/*     */   public String getId() {
/* 177 */     return this.id;
/*     */   }
/*     */ 
/*     */   public Role setId(String id) {
/* 181 */     this.id = id;
/* 182 */     return this;
/*     */   }
/*     */ 
/*     */   public void unsetId() {
/* 186 */     this.id = null;
/*     */   }
/*     */ 
/*     */   public boolean isSetId()
/*     */   {
/* 191 */     return this.id != null;
/*     */   }
/*     */ 
/*     */   public void setIdIsSet(boolean value) {
/* 195 */     if (!value)
/* 196 */       this.id = null;
/*     */   }
/*     */ 
/*     */   public String getServiceId()
/*     */   {
/* 201 */     return this.serviceId;
/*     */   }
/*     */ 
/*     */   public Role setServiceId(String serviceId) {
/* 205 */     this.serviceId = serviceId;
/* 206 */     return this;
/*     */   }
/*     */ 
/*     */   public void unsetServiceId() {
/* 210 */     this.serviceId = null;
/*     */   }
/*     */ 
/*     */   public boolean isSetServiceId()
/*     */   {
/* 215 */     return this.serviceId != null;
/*     */   }
/*     */ 
/*     */   public void setServiceIdIsSet(boolean value) {
/* 219 */     if (!value)
/* 220 */       this.serviceId = null;
/*     */   }
/*     */ 
/*     */   public String getName()
/*     */   {
/* 225 */     return this.name;
/*     */   }
/*     */ 
/*     */   public Role setName(String name) {
/* 229 */     this.name = name;
/* 230 */     return this;
/*     */   }
/*     */ 
/*     */   public void unsetName() {
/* 234 */     this.name = null;
/*     */   }
/*     */ 
/*     */   public boolean isSetName()
/*     */   {
/* 239 */     return this.name != null;
/*     */   }
/*     */ 
/*     */   public void setNameIsSet(boolean value) {
/* 243 */     if (!value)
/* 244 */       this.name = null;
/*     */   }
/*     */ 
/*     */   public String getTenantId()
/*     */   {
/* 249 */     return this.tenantId;
/*     */   }
/*     */ 
/*     */   public Role setTenantId(String tenantId) {
/* 253 */     this.tenantId = tenantId;
/* 254 */     return this;
/*     */   }
/*     */ 
/*     */   public void unsetTenantId() {
/* 258 */     this.tenantId = null;
/*     */   }
/*     */ 
/*     */   public boolean isSetTenantId()
/*     */   {
/* 263 */     return this.tenantId != null;
/*     */   }
/*     */ 
/*     */   public void setTenantIdIsSet(boolean value) {
/* 267 */     if (!value)
/* 268 */       this.tenantId = null;
/*     */   }
/*     */ 
/*     */   public void setFieldValue(_Fields field, Object value)
/*     */   {
/* 273 */     switch (field.ordinal()) {//1.$SwitchMap$com$hp$csbu$cc$security$cs$thrift$service$Role$_Fields[field.ordinal()]) {
/*     */     case 1:
/* 275 */       if (value == null)
/* 276 */         unsetId();
/*     */       else {
/* 278 */         setId((String)value);
/*     */       }
/* 280 */       break;
/*     */     case 2:
/* 283 */       if (value == null)
/* 284 */         unsetServiceId();
/*     */       else {
/* 286 */         setServiceId((String)value);
/*     */       }
/* 288 */       break;
/*     */     case 3:
/* 291 */       if (value == null)
/* 292 */         unsetName();
/*     */       else {
/* 294 */         setName((String)value);
/*     */       }
/* 296 */       break;
/*     */     case 4:
/* 299 */       if (value == null)
/* 300 */         unsetTenantId();
/*     */       else
/* 302 */         setTenantId((String)value);
/*     */       break;
/*     */     }
/*     */   }
/*     */ 
/*     */   public Object getFieldValue(_Fields field)
/*     */   {
/* 310 */     switch (field.ordinal()) {//1.$SwitchMap$com$hp$csbu$cc$security$cs$thrift$service$Role$_Fields[field.ordinal()]) {
/*     */     case 1:
/* 312 */       return getId();
/*     */     case 2:
/* 315 */       return getServiceId();
/*     */     case 3:
/* 318 */       return getName();
/*     */     case 4:
/* 321 */       return getTenantId();
/*     */     }
/*     */ 
/* 324 */     throw new IllegalStateException();
/*     */   }
/*     */ 
/*     */   public boolean isSet(_Fields field)
/*     */   {
/* 329 */     if (field == null) {
/* 330 */       throw new IllegalArgumentException();
/*     */     }
/*     */ 
/* 333 */     switch (field.ordinal()) { //1.$SwitchMap$com$hp$csbu$cc$security$cs$thrift$service$Role$_Fields[field.ordinal()]) {
/*     */     case 1:
/* 335 */       return isSetId();
/*     */     case 2:
/* 337 */       return isSetServiceId();
/*     */     case 3:
/* 339 */       return isSetName();
/*     */     case 4:
/* 341 */       return isSetTenantId();
/*     */     }
/* 343 */     throw new IllegalStateException();
/*     */   }
/*     */ 
/*     */   public boolean equals(Object that)
/*     */   {
/* 348 */     if (that == null)
/* 349 */       return false;
/* 350 */     if ((that instanceof Role))
/* 351 */       return equals((Role)that);
/* 352 */     return false;
/*     */   }
/*     */ 
/*     */   public boolean equals(Role that) {
/* 356 */     if (that == null) {
/* 357 */       return false;
/*     */     }
/* 359 */     boolean this_present_id = isSetId();
/* 360 */     boolean that_present_id = that.isSetId();
/* 361 */     if ((this_present_id) || (that_present_id)) {
/* 362 */       if ((!this_present_id) || (!that_present_id))
/* 363 */         return false;
/* 364 */       if (!this.id.equals(that.id)) {
/* 365 */         return false;
/*     */       }
/*     */     }
/* 368 */     boolean this_present_serviceId = isSetServiceId();
/* 369 */     boolean that_present_serviceId = that.isSetServiceId();
/* 370 */     if ((this_present_serviceId) || (that_present_serviceId)) {
/* 371 */       if ((!this_present_serviceId) || (!that_present_serviceId))
/* 372 */         return false;
/* 373 */       if (!this.serviceId.equals(that.serviceId)) {
/* 374 */         return false;
/*     */       }
/*     */     }
/* 377 */     boolean this_present_name = isSetName();
/* 378 */     boolean that_present_name = that.isSetName();
/* 379 */     if ((this_present_name) || (that_present_name)) {
/* 380 */       if ((!this_present_name) || (!that_present_name))
/* 381 */         return false;
/* 382 */       if (!this.name.equals(that.name)) {
/* 383 */         return false;
/*     */       }
/*     */     }
/* 386 */     boolean this_present_tenantId = isSetTenantId();
/* 387 */     boolean that_present_tenantId = that.isSetTenantId();
/* 388 */     if ((this_present_tenantId) || (that_present_tenantId)) {
/* 389 */       if ((!this_present_tenantId) || (!that_present_tenantId))
/* 390 */         return false;
/* 391 */       if (!this.tenantId.equals(that.tenantId)) {
/* 392 */         return false;
/*     */       }
/*     */     }
/* 395 */     return true;
/*     */   }
/*     */ 
/*     */   public int hashCode()
/*     */   {
/* 400 */     return 0;
/*     */   }
/*     */ 
/*     */   public int compareTo(Role other) {
/* 404 */     if (!getClass().equals(other.getClass())) {
/* 405 */       return getClass().getName().compareTo(other.getClass().getName());
/*     */     }
/*     */ 
/* 408 */     int lastComparison = 0;
/* 409 */     Role typedOther = other;
/*     */ 
/* 411 */     lastComparison = Boolean.valueOf(isSetId()).compareTo(Boolean.valueOf(typedOther.isSetId()));
/* 412 */     if (lastComparison != 0) {
/* 413 */       return lastComparison;
/*     */     }
/* 415 */     if (isSetId()) {
/* 416 */       lastComparison = TBaseHelper.compareTo(this.id, typedOther.id);
/* 417 */       if (lastComparison != 0) {
/* 418 */         return lastComparison;
/*     */       }
/*     */     }
/* 421 */     lastComparison = Boolean.valueOf(isSetServiceId()).compareTo(Boolean.valueOf(typedOther.isSetServiceId()));
/* 422 */     if (lastComparison != 0) {
/* 423 */       return lastComparison;
/*     */     }
/* 425 */     if (isSetServiceId()) {
/* 426 */       lastComparison = TBaseHelper.compareTo(this.serviceId, typedOther.serviceId);
/* 427 */       if (lastComparison != 0) {
/* 428 */         return lastComparison;
/*     */       }
/*     */     }
/* 431 */     lastComparison = Boolean.valueOf(isSetName()).compareTo(Boolean.valueOf(typedOther.isSetName()));
/* 432 */     if (lastComparison != 0) {
/* 433 */       return lastComparison;
/*     */     }
/* 435 */     if (isSetName()) {
/* 436 */       lastComparison = TBaseHelper.compareTo(this.name, typedOther.name);
/* 437 */       if (lastComparison != 0) {
/* 438 */         return lastComparison;
/*     */       }
/*     */     }
/* 441 */     lastComparison = Boolean.valueOf(isSetTenantId()).compareTo(Boolean.valueOf(typedOther.isSetTenantId()));
/* 442 */     if (lastComparison != 0) {
/* 443 */       return lastComparison;
/*     */     }
/* 445 */     if (isSetTenantId()) {
/* 446 */       lastComparison = TBaseHelper.compareTo(this.tenantId, typedOther.tenantId);
/* 447 */       if (lastComparison != 0) {
/* 448 */         return lastComparison;
/*     */       }
/*     */     }
/* 451 */     return 0;
/*     */   }
/*     */ 
/*     */   public _Fields fieldForId(int fieldId) {
/* 455 */     return _Fields.findByThriftId(fieldId);
/*     */   }
/*     */ 
/*     */   public void read(TProtocol iprot) throws TException {
/* 459 */     ((SchemeFactory)schemes.get(iprot.getScheme())).getScheme().read(iprot, this);
/*     */   }
/*     */ 
/*     */   public void write(TProtocol oprot) throws TException {
/* 463 */     ((SchemeFactory)schemes.get(oprot.getScheme())).getScheme().write(oprot, this);
/*     */   }
/*     */ 
/*     */   public String toString()
/*     */   {
/* 468 */     StringBuilder sb = new StringBuilder("Role(");
/* 469 */     boolean first = true;
/*     */ 
/* 471 */     sb.append("id:");
/* 472 */     if (this.id == null)
/* 473 */       sb.append("null");
/*     */     else {
/* 475 */       sb.append(this.id);
/*     */     }
/* 477 */     first = false;
/* 478 */     if (isSetServiceId()) {
/* 479 */       if (!first) sb.append(", ");
/* 480 */       sb.append("serviceId:");
/* 481 */       if (this.serviceId == null)
/* 482 */         sb.append("null");
/*     */       else {
/* 484 */         sb.append(this.serviceId);
/*     */       }
/* 486 */       first = false;
/*     */     }
/* 488 */     if (isSetName()) {
/* 489 */       if (!first) sb.append(", ");
/* 490 */       sb.append("name:");
/* 491 */       if (this.name == null)
/* 492 */         sb.append("null");
/*     */       else {
/* 494 */         sb.append(this.name);
/*     */       }
/* 496 */       first = false;
/*     */     }
/* 498 */     if (isSetTenantId()) {
/* 499 */       if (!first) sb.append(", ");
/* 500 */       sb.append("tenantId:");
/* 501 */       if (this.tenantId == null)
/* 502 */         sb.append("null");
/*     */       else {
/* 504 */         sb.append(this.tenantId);
/*     */       }
/* 506 */       first = false;
/*     */     }
/* 508 */     sb.append(")");
/* 509 */     return sb.toString();
/*     */   }
/*     */ 
/*     */   public void validate() throws TException
/*     */   {
/*     */   }
/*     */ 
/*     */   private void writeObject(ObjectOutputStream out) throws IOException {
/*     */     try {
/* 518 */       write(new TCompactProtocol(new TIOStreamTransport(out)));
/*     */     } catch (TException te) {
/* 520 */       throw new IOException(te);
/*     */     }
/*     */   }
/*     */ 
/*     */   private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
/*     */     try {
/* 526 */       read(new TCompactProtocol(new TIOStreamTransport(in)));
/*     */     } catch (TException te) {
/* 528 */       throw new IOException(te);
/*     */     }
/*     */   }
/*     */ 
/*     */   static
/*     */   {
/*  43 */     schemes.put(StandardScheme.class, new RoleStandardSchemeFactory());
/*  44 */     schemes.put(TupleScheme.class, new RoleTupleSchemeFactory());
/*     */ 
/* 123 */     Map tmpMap = new EnumMap(_Fields.class);
/* 124 */     tmpMap.put(_Fields.ID, new FieldMetaData("id", (byte)3, new FieldValueMetaData((byte)11)));
/*     */ 
/* 126 */     tmpMap.put(_Fields.SERVICE_ID, new FieldMetaData("serviceId", (byte)2, new FieldValueMetaData((byte)11)));
/*     */ 
/* 128 */     tmpMap.put(_Fields.NAME, new FieldMetaData("name", (byte)2, new FieldValueMetaData((byte)11)));
/*     */ 
/* 130 */     tmpMap.put(_Fields.TENANT_ID, new FieldMetaData("tenantId", (byte)2, new FieldValueMetaData((byte)11)));
/*     */ 
/* 132 */     metaDataMap = Collections.unmodifiableMap(tmpMap);
/* 133 */     FieldMetaData.addStructMetaDataMap(Role.class, metaDataMap);
/*     */   }
/*     */ 
/*     */   private static class RoleTupleScheme extends TupleScheme<Role>
/*     */   {
/*     */     public void write(TProtocol prot, Role struct)
/*     */       throws TException
/*     */     {
/* 639 */       TTupleProtocol oprot = (TTupleProtocol)prot;
/* 640 */       BitSet optionals = new BitSet();
/* 641 */       if (struct.isSetId()) {
/* 642 */         optionals.set(0);
/*     */       }
/* 644 */       if (struct.isSetServiceId()) {
/* 645 */         optionals.set(1);
/*     */       }
/* 647 */       if (struct.isSetName()) {
/* 648 */         optionals.set(2);
/*     */       }
/* 650 */       if (struct.isSetTenantId()) {
/* 651 */         optionals.set(3);
/*     */       }
/* 653 */       oprot.writeBitSet(optionals, 4);
/* 654 */       if (struct.isSetId()) {
/* 655 */         oprot.writeString(struct.id);
/*     */       }
/* 657 */       if (struct.isSetServiceId()) {
/* 658 */         oprot.writeString(struct.serviceId);
/*     */       }
/* 660 */       if (struct.isSetName()) {
/* 661 */         oprot.writeString(struct.name);
/*     */       }
/* 663 */       if (struct.isSetTenantId())
/* 664 */         oprot.writeString(struct.tenantId);
/*     */     }
/*     */ 
/*     */     public void read(TProtocol prot, Role struct)
/*     */       throws TException
/*     */     {
/* 670 */       TTupleProtocol iprot = (TTupleProtocol)prot;
/* 671 */       BitSet incoming = iprot.readBitSet(4);
/* 672 */       if (incoming.get(0)) {
/* 673 */         struct.id = iprot.readString();
/* 674 */         struct.setIdIsSet(true);
/*     */       }
/* 676 */       if (incoming.get(1)) {
/* 677 */         struct.serviceId = iprot.readString();
/* 678 */         struct.setServiceIdIsSet(true);
/*     */       }
/* 680 */       if (incoming.get(2)) {
/* 681 */         struct.name = iprot.readString();
/* 682 */         struct.setNameIsSet(true);
/*     */       }
/* 684 */       if (incoming.get(3)) {
/* 685 */         struct.tenantId = iprot.readString();
/* 686 */         struct.setTenantIdIsSet(true);
/*     */       }
/*     */     }
/*     */   }
/*     */ 
/*     */   private static class RoleTupleSchemeFactory
/*     */     implements SchemeFactory
/*     */   {
/*     */     public Role.RoleTupleScheme getScheme()
/*     */     {
/* 631 */       return new Role.RoleTupleScheme();
/*     */     }
/*     */   }
/*     */ 
/*     */   private static class RoleStandardScheme extends StandardScheme<Role>
/*     */   {
/*     */     public void read(TProtocol iprot, Role struct)
/*     */       throws TException
/*     */     {
/* 542 */       iprot.readStructBegin();
/*     */       while (true)
/*     */       {
/* 545 */         TField schemeField = iprot.readFieldBegin();
/* 546 */         if (schemeField.type == 0) {
/*     */           break;
/*     */         }
/* 549 */         switch (schemeField.id) {
/*     */         case 1:
/* 551 */           if (schemeField.type == 11) {
/* 552 */             struct.id = iprot.readString();
/* 553 */             struct.setIdIsSet(true);
/*     */           } else {
/* 555 */             TProtocolUtil.skip(iprot, schemeField.type);
/*     */           }
/* 557 */           break;
/*     */         case 2:
/* 559 */           if (schemeField.type == 11) {
/* 560 */             struct.serviceId = iprot.readString();
/* 561 */             struct.setServiceIdIsSet(true);
/*     */           } else {
/* 563 */             TProtocolUtil.skip(iprot, schemeField.type);
/*     */           }
/* 565 */           break;
/*     */         case 3:
/* 567 */           if (schemeField.type == 11) {
/* 568 */             struct.name = iprot.readString();
/* 569 */             struct.setNameIsSet(true);
/*     */           } else {
/* 571 */             TProtocolUtil.skip(iprot, schemeField.type);
/*     */           }
/* 573 */           break;
/*     */         case 4:
/* 575 */           if (schemeField.type == 11) {
/* 576 */             struct.tenantId = iprot.readString();
/* 577 */             struct.setTenantIdIsSet(true);
/*     */           } else {
/* 579 */             TProtocolUtil.skip(iprot, schemeField.type);
/*     */           }
/* 581 */           break;
/*     */         default:
/* 583 */           TProtocolUtil.skip(iprot, schemeField.type);
/*     */         }
/* 585 */         iprot.readFieldEnd();
/*     */       }
/* 587 */       iprot.readStructEnd();
/*     */ 
/* 590 */       struct.validate();
/*     */     }
/*     */ 
/*     */     public void write(TProtocol oprot, Role struct) throws TException {
/* 594 */       struct.validate();
/*     */ 
/* 596 */       oprot.writeStructBegin(Role.STRUCT_DESC);
/* 597 */       if (struct.id != null) {
/* 598 */         oprot.writeFieldBegin(Role.ID_FIELD_DESC);
/* 599 */         oprot.writeString(struct.id);
/* 600 */         oprot.writeFieldEnd();
/*     */       }
/* 602 */       if ((struct.serviceId != null) && 
/* 603 */         (struct.isSetServiceId())) {
/* 604 */         oprot.writeFieldBegin(Role.SERVICE_ID_FIELD_DESC);
/* 605 */         oprot.writeString(struct.serviceId);
/* 606 */         oprot.writeFieldEnd();
/*     */       }
/*     */ 
/* 609 */       if ((struct.name != null) && 
/* 610 */         (struct.isSetName())) {
/* 611 */         oprot.writeFieldBegin(Role.NAME_FIELD_DESC);
/* 612 */         oprot.writeString(struct.name);
/* 613 */         oprot.writeFieldEnd();
/*     */       }
/*     */ 
/* 616 */       if ((struct.tenantId != null) && 
/* 617 */         (struct.isSetTenantId())) {
/* 618 */         oprot.writeFieldBegin(Role.TENANT_ID_FIELD_DESC);
/* 619 */         oprot.writeString(struct.tenantId);
/* 620 */         oprot.writeFieldEnd();
/*     */       }
/*     */ 
/* 623 */       oprot.writeFieldStop();
/* 624 */       oprot.writeStructEnd();
/*     */     }
/*     */   }
/*     */ 
/*     */   private static class RoleStandardSchemeFactory
/*     */     implements SchemeFactory
/*     */   {
/*     */     public Role.RoleStandardScheme getScheme()
/*     */     {
/* 534 */       return new Role.RoleStandardScheme();
/*     */     }
/*     */   }
/*     */ 
/*     */   public static enum _Fields
/*     */     implements TFieldIdEnum
/*     */   {
/*  54 */     ID((short)1, "id"), 
/*  55 */     SERVICE_ID((short)2, "serviceId"), 
/*  56 */     NAME((short)3, "name"), 
/*  57 */     TENANT_ID((short)4, "tenantId");
/*     */ 
/*     */     private static final Map<String, _Fields> byName;
/*     */     private final short _thriftId;
/*     */     private final String _fieldName;
/*     */ 
/*     */     public static _Fields findByThriftId(int fieldId)
/*     */     {
/*  71 */       switch (fieldId) {
/*     */       case 1:
/*  73 */         return ID;
/*     */       case 2:
/*  75 */         return SERVICE_ID;
/*     */       case 3:
/*  77 */         return NAME;
/*     */       case 4:
/*  79 */         return TENANT_ID;
/*     */       }
/*  81 */       return null;
/*     */     }
/*     */ 
/*     */     public static _Fields findByThriftIdOrThrow(int fieldId)
/*     */     {
/*  90 */       _Fields fields = findByThriftId(fieldId);
/*  91 */       if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
/*  92 */       return fields;
/*     */     }
/*     */ 
/*     */     public static _Fields findByName(String name)
/*     */     {
/*  99 */       return (_Fields)byName.get(name);
/*     */     }
/*     */ 
/*     */     private _Fields(short thriftId, String fieldName)
/*     */     {
/* 106 */       this._thriftId = thriftId;
/* 107 */       this._fieldName = fieldName;
/*     */     }
/*     */ 
/*     */     public short getThriftFieldId() {
/* 111 */       return this._thriftId;
/*     */     }
/*     */ 
/*     */     public String getFieldName() {
/* 115 */       return this._fieldName;
/*     */     }
/*     */ 
/*     */     static
/*     */     {
/*  59 */       byName = new HashMap();
/*     */ 
/*  62 */       for (_Fields field : EnumSet.allOf(_Fields.class))
/*  63 */         byName.put(field.getFieldName(), field);
/*     */     }
/*     */   }
/*     */ }

/* Location:           /Users/johnderr/.m2/repository/com/hp/csbu/cc/CsThriftModel/1.2-SNAPSHOT/CsThriftModel-1.2-20140130.160951-1223.jar
 * Qualified Name:     com.hp.csbu.cc.security.cs.thrift.service.Role
 * JD-Core Version:    0.6.2
 */