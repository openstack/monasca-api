/*     */ package com.hp.csbu.cc.security.cs.thrift.service;
/*     */ 
/*     */ import java.io.IOException;
/*     */ import java.io.ObjectInputStream;
/*     */ import java.io.ObjectOutputStream;
/*     */ import java.io.Serializable;
/*     */ import java.util.ArrayList;
/*     */ import java.util.BitSet;
/*     */ import java.util.Collections;
/*     */ import java.util.EnumMap;
/*     */ import java.util.EnumSet;
/*     */ import java.util.HashMap;
/*     */ import java.util.Iterator;
/*     */ import java.util.List;
/*     */ import java.util.Map;
/*     */ import org.apache.thrift.TBase;
/*     */ import org.apache.thrift.TBaseHelper;
/*     */ import org.apache.thrift.TException;
/*     */ import org.apache.thrift.TFieldIdEnum;
/*     */ import org.apache.thrift.meta_data.FieldMetaData;
/*     */ import org.apache.thrift.meta_data.FieldValueMetaData;
/*     */ import org.apache.thrift.meta_data.ListMetaData;
/*     */ import org.apache.thrift.meta_data.StructMetaData;
/*     */ import org.apache.thrift.protocol.TCompactProtocol;
/*     */ import org.apache.thrift.protocol.TField;
/*     */ import org.apache.thrift.protocol.TList;
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
/*     */ public class User
/*     */   implements TBase<User, User._Fields>, Serializable, Cloneable
/*     */ {
/*  36 */   private static final TStruct STRUCT_DESC = new TStruct("User");
/*     */ 
/*  38 */   private static final TField USERNAME_FIELD_DESC = new TField("username", (byte)11, (short)1);
/*  39 */   private static final TField TENANT_ID_FIELD_DESC = new TField("tenantId", (byte)11, (short)2);
/*  40 */   private static final TField ROLES_FIELD_DESC = new TField("roles", (byte)15, (short)3);
/*  41 */   private static final TField USER_ID_FIELD_DESC = new TField("userId", (byte)11, (short)4);
/*  42 */   private static final TField TENANT_NAME_FIELD_DESC = new TField("tenantName", (byte)11, (short)5);
/*  43 */   private static final TField SWIFT_ACCOUNT_HASH_FIELD_DESC = new TField("swiftAccountHash", (byte)11, (short)6);
/*     */ 
/*  45 */   private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap();
/*     */   public String username;
/*     */   public String tenantId;
/*     */   public List<Role> roles;
/*     */   public String userId;
/*     */   public String tenantName;
/*     */   public String swiftAccountHash;
/* 132 */   private _Fields[] optionals = { _Fields.USERNAME, _Fields.TENANT_ID, _Fields.ROLES, _Fields.USER_ID, _Fields.TENANT_NAME, _Fields.SWIFT_ACCOUNT_HASH };
/*     */   public static final Map<_Fields, FieldMetaData> metaDataMap;
/*     */ 
/*     */   public User()
/*     */   {
/*     */   }
/*     */ 
/*     */   public User(User other)
/*     */   {
/* 160 */     if (other.isSetUsername()) {
/* 161 */       this.username = other.username;
/*     */     }
/* 163 */     if (other.isSetTenantId()) {
/* 164 */       this.tenantId = other.tenantId;
/*     */     }
/* 166 */     if (other.isSetRoles()) {
/* 167 */       List __this__roles = new ArrayList();
/* 168 */       for (Role other_element : other.roles) {
/* 169 */         __this__roles.add(new Role(other_element));
/*     */       }
/* 171 */       this.roles = __this__roles;
/*     */     }
/* 173 */     if (other.isSetUserId()) {
/* 174 */       this.userId = other.userId;
/*     */     }
/* 176 */     if (other.isSetTenantName()) {
/* 177 */       this.tenantName = other.tenantName;
/*     */     }
/* 179 */     if (other.isSetSwiftAccountHash())
/* 180 */       this.swiftAccountHash = other.swiftAccountHash;
/*     */   }
/*     */ 
/*     */   public User deepCopy()
/*     */   {
/* 185 */     return new User(this);
/*     */   }
/*     */ 
/*     */   public void clear()
/*     */   {
/* 190 */     this.username = null;
/* 191 */     this.tenantId = null;
/* 192 */     this.roles = null;
/* 193 */     this.userId = null;
/* 194 */     this.tenantName = null;
/* 195 */     this.swiftAccountHash = null;
/*     */   }
/*     */ 
/*     */   public String getUsername() {
/* 199 */     return this.username;
/*     */   }
/*     */ 
/*     */   public User setUsername(String username) {
/* 203 */     this.username = username;
/* 204 */     return this;
/*     */   }
/*     */ 
/*     */   public void unsetUsername() {
/* 208 */     this.username = null;
/*     */   }
/*     */ 
/*     */   public boolean isSetUsername()
/*     */   {
/* 213 */     return this.username != null;
/*     */   }
/*     */ 
/*     */   public void setUsernameIsSet(boolean value) {
/* 217 */     if (!value)
/* 218 */       this.username = null;
/*     */   }
/*     */ 
/*     */   public String getTenantId()
/*     */   {
/* 223 */     return this.tenantId;
/*     */   }
/*     */ 
/*     */   public User setTenantId(String tenantId) {
/* 227 */     this.tenantId = tenantId;
/* 228 */     return this;
/*     */   }
/*     */ 
/*     */   public void unsetTenantId() {
/* 232 */     this.tenantId = null;
/*     */   }
/*     */ 
/*     */   public boolean isSetTenantId()
/*     */   {
/* 237 */     return this.tenantId != null;
/*     */   }
/*     */ 
/*     */   public void setTenantIdIsSet(boolean value) {
/* 241 */     if (!value)
/* 242 */       this.tenantId = null;
/*     */   }
/*     */ 
/*     */   public int getRolesSize()
/*     */   {
/* 247 */     return this.roles == null ? 0 : this.roles.size();
/*     */   }
/*     */ 
/*     */   public Iterator<Role> getRolesIterator() {
/* 251 */     return this.roles == null ? null : this.roles.iterator();
/*     */   }
/*     */ 
/*     */   public void addToRoles(Role elem) {
/* 255 */     if (this.roles == null) {
/* 256 */       this.roles = new ArrayList();
/*     */     }
/* 258 */     this.roles.add(elem);
/*     */   }
/*     */ 
/*     */   public List<Role> getRoles() {
/* 262 */     return this.roles;
/*     */   }
/*     */ 
/*     */   public User setRoles(List<Role> roles) {
/* 266 */     this.roles = roles;
/* 267 */     return this;
/*     */   }
/*     */ 
/*     */   public void unsetRoles() {
/* 271 */     this.roles = null;
/*     */   }
/*     */ 
/*     */   public boolean isSetRoles()
/*     */   {
/* 276 */     return this.roles != null;
/*     */   }
/*     */ 
/*     */   public void setRolesIsSet(boolean value) {
/* 280 */     if (!value)
/* 281 */       this.roles = null;
/*     */   }
/*     */ 
/*     */   public String getUserId()
/*     */   {
/* 286 */     return this.userId;
/*     */   }
/*     */ 
/*     */   public User setUserId(String userId) {
/* 290 */     this.userId = userId;
/* 291 */     return this;
/*     */   }
/*     */ 
/*     */   public void unsetUserId() {
/* 295 */     this.userId = null;
/*     */   }
/*     */ 
/*     */   public boolean isSetUserId()
/*     */   {
/* 300 */     return this.userId != null;
/*     */   }
/*     */ 
/*     */   public void setUserIdIsSet(boolean value) {
/* 304 */     if (!value)
/* 305 */       this.userId = null;
/*     */   }
/*     */ 
/*     */   public String getTenantName()
/*     */   {
/* 310 */     return this.tenantName;
/*     */   }
/*     */ 
/*     */   public User setTenantName(String tenantName) {
/* 314 */     this.tenantName = tenantName;
/* 315 */     return this;
/*     */   }
/*     */ 
/*     */   public void unsetTenantName() {
/* 319 */     this.tenantName = null;
/*     */   }
/*     */ 
/*     */   public boolean isSetTenantName()
/*     */   {
/* 324 */     return this.tenantName != null;
/*     */   }
/*     */ 
/*     */   public void setTenantNameIsSet(boolean value) {
/* 328 */     if (!value)
/* 329 */       this.tenantName = null;
/*     */   }
/*     */ 
/*     */   public String getSwiftAccountHash()
/*     */   {
/* 334 */     return this.swiftAccountHash;
/*     */   }
/*     */ 
/*     */   public User setSwiftAccountHash(String swiftAccountHash) {
/* 338 */     this.swiftAccountHash = swiftAccountHash;
/* 339 */     return this;
/*     */   }
/*     */ 
/*     */   public void unsetSwiftAccountHash() {
/* 343 */     this.swiftAccountHash = null;
/*     */   }
/*     */ 
/*     */   public boolean isSetSwiftAccountHash()
/*     */   {
/* 348 */     return this.swiftAccountHash != null;
/*     */   }
/*     */ 
/*     */   public void setSwiftAccountHashIsSet(boolean value) {
/* 352 */     if (!value)
/* 353 */       this.swiftAccountHash = null;
/*     */   }
/*     */ 
/*     */   public void setFieldValue(_Fields field, Object value)
/*     */   {
/* 358 */     switch (field.ordinal()) { //1.$SwitchMap$com$hp$csbu$cc$security$cs$thrift$service$User$_Fields[field.ordinal()]) {
/*     */     case 1:
/* 360 */       if (value == null)
/* 361 */         unsetUsername();
/*     */       else {
/* 363 */         setUsername((String)value);
/*     */       }
/* 365 */       break;
/*     */     case 2:
/* 368 */       if (value == null)
/* 369 */         unsetTenantId();
/*     */       else {
/* 371 */         setTenantId((String)value);
/*     */       }
/* 373 */       break;
/*     */     case 3:
/* 376 */       if (value == null)
/* 377 */         unsetRoles();
/*     */       else {
/* 379 */         setRoles((List)value);
/*     */       }
/* 381 */       break;
/*     */     case 4:
/* 384 */       if (value == null)
/* 385 */         unsetUserId();
/*     */       else {
/* 387 */         setUserId((String)value);
/*     */       }
/* 389 */       break;
/*     */     case 5:
/* 392 */       if (value == null)
/* 393 */         unsetTenantName();
/*     */       else {
/* 395 */         setTenantName((String)value);
/*     */       }
/* 397 */       break;
/*     */     case 6:
/* 400 */       if (value == null)
/* 401 */         unsetSwiftAccountHash();
/*     */       else
/* 403 */         setSwiftAccountHash((String)value);
/*     */       break;
/*     */     }
/*     */   }
/*     */ 
/*     */   public Object getFieldValue(_Fields field)
/*     */   {
/* 411 */     switch (field.ordinal()) {//1.$SwitchMap$com$hp$csbu$cc$security$cs$thrift$service$User$_Fields[field.ordinal()]) {
/*     */     case 1:
/* 413 */       return getUsername();
/*     */     case 2:
/* 416 */       return getTenantId();
/*     */     case 3:
/* 419 */       return getRoles();
/*     */     case 4:
/* 422 */       return getUserId();
/*     */     case 5:
/* 425 */       return getTenantName();
/*     */     case 6:
/* 428 */       return getSwiftAccountHash();
/*     */     }
/*     */ 
/* 431 */     throw new IllegalStateException();
/*     */   }
/*     */ 
/*     */   public boolean isSet(_Fields field)
/*     */   {
/* 436 */     if (field == null) {
/* 437 */       throw new IllegalArgumentException();
/*     */     }
/*     */ 
/* 440 */     switch (field.ordinal()) {//1.$SwitchMap$com$hp$csbu$cc$security$cs$thrift$service$User$_Fields[field.ordinal()]) {
/*     */     case 1:
/* 442 */       return isSetUsername();
/*     */     case 2:
/* 444 */       return isSetTenantId();
/*     */     case 3:
/* 446 */       return isSetRoles();
/*     */     case 4:
/* 448 */       return isSetUserId();
/*     */     case 5:
/* 450 */       return isSetTenantName();
/*     */     case 6:
/* 452 */       return isSetSwiftAccountHash();
/*     */     }
/* 454 */     throw new IllegalStateException();
/*     */   }
/*     */ 
/*     */   public boolean equals(Object that)
/*     */   {
/* 459 */     if (that == null)
/* 460 */       return false;
/* 461 */     if ((that instanceof User))
/* 462 */       return equals((User)that);
/* 463 */     return false;
/*     */   }
/*     */ 
/*     */   public boolean equals(User that) {
/* 467 */     if (that == null) {
/* 468 */       return false;
/*     */     }
/* 470 */     boolean this_present_username = isSetUsername();
/* 471 */     boolean that_present_username = that.isSetUsername();
/* 472 */     if ((this_present_username) || (that_present_username)) {
/* 473 */       if ((!this_present_username) || (!that_present_username))
/* 474 */         return false;
/* 475 */       if (!this.username.equals(that.username)) {
/* 476 */         return false;
/*     */       }
/*     */     }
/* 479 */     boolean this_present_tenantId = isSetTenantId();
/* 480 */     boolean that_present_tenantId = that.isSetTenantId();
/* 481 */     if ((this_present_tenantId) || (that_present_tenantId)) {
/* 482 */       if ((!this_present_tenantId) || (!that_present_tenantId))
/* 483 */         return false;
/* 484 */       if (!this.tenantId.equals(that.tenantId)) {
/* 485 */         return false;
/*     */       }
/*     */     }
/* 488 */     boolean this_present_roles = isSetRoles();
/* 489 */     boolean that_present_roles = that.isSetRoles();
/* 490 */     if ((this_present_roles) || (that_present_roles)) {
/* 491 */       if ((!this_present_roles) || (!that_present_roles))
/* 492 */         return false;
/* 493 */       if (!this.roles.equals(that.roles)) {
/* 494 */         return false;
/*     */       }
/*     */     }
/* 497 */     boolean this_present_userId = isSetUserId();
/* 498 */     boolean that_present_userId = that.isSetUserId();
/* 499 */     if ((this_present_userId) || (that_present_userId)) {
/* 500 */       if ((!this_present_userId) || (!that_present_userId))
/* 501 */         return false;
/* 502 */       if (!this.userId.equals(that.userId)) {
/* 503 */         return false;
/*     */       }
/*     */     }
/* 506 */     boolean this_present_tenantName = isSetTenantName();
/* 507 */     boolean that_present_tenantName = that.isSetTenantName();
/* 508 */     if ((this_present_tenantName) || (that_present_tenantName)) {
/* 509 */       if ((!this_present_tenantName) || (!that_present_tenantName))
/* 510 */         return false;
/* 511 */       if (!this.tenantName.equals(that.tenantName)) {
/* 512 */         return false;
/*     */       }
/*     */     }
/* 515 */     boolean this_present_swiftAccountHash = isSetSwiftAccountHash();
/* 516 */     boolean that_present_swiftAccountHash = that.isSetSwiftAccountHash();
/* 517 */     if ((this_present_swiftAccountHash) || (that_present_swiftAccountHash)) {
/* 518 */       if ((!this_present_swiftAccountHash) || (!that_present_swiftAccountHash))
/* 519 */         return false;
/* 520 */       if (!this.swiftAccountHash.equals(that.swiftAccountHash)) {
/* 521 */         return false;
/*     */       }
/*     */     }
/* 524 */     return true;
/*     */   }
/*     */ 
/*     */   public int hashCode()
/*     */   {
/* 529 */     return 0;
/*     */   }
/*     */ 
/*     */   public int compareTo(User other) {
/* 533 */     if (!getClass().equals(other.getClass())) {
/* 534 */       return getClass().getName().compareTo(other.getClass().getName());
/*     */     }
/*     */ 
/* 537 */     int lastComparison = 0;
/* 538 */     User typedOther = other;
/*     */ 
/* 540 */     lastComparison = Boolean.valueOf(isSetUsername()).compareTo(Boolean.valueOf(typedOther.isSetUsername()));
/* 541 */     if (lastComparison != 0) {
/* 542 */       return lastComparison;
/*     */     }
/* 544 */     if (isSetUsername()) {
/* 545 */       lastComparison = TBaseHelper.compareTo(this.username, typedOther.username);
/* 546 */       if (lastComparison != 0) {
/* 547 */         return lastComparison;
/*     */       }
/*     */     }
/* 550 */     lastComparison = Boolean.valueOf(isSetTenantId()).compareTo(Boolean.valueOf(typedOther.isSetTenantId()));
/* 551 */     if (lastComparison != 0) {
/* 552 */       return lastComparison;
/*     */     }
/* 554 */     if (isSetTenantId()) {
/* 555 */       lastComparison = TBaseHelper.compareTo(this.tenantId, typedOther.tenantId);
/* 556 */       if (lastComparison != 0) {
/* 557 */         return lastComparison;
/*     */       }
/*     */     }
/* 560 */     lastComparison = Boolean.valueOf(isSetRoles()).compareTo(Boolean.valueOf(typedOther.isSetRoles()));
/* 561 */     if (lastComparison != 0) {
/* 562 */       return lastComparison;
/*     */     }
/* 564 */     if (isSetRoles()) {
/* 565 */       lastComparison = TBaseHelper.compareTo(this.roles, typedOther.roles);
/* 566 */       if (lastComparison != 0) {
/* 567 */         return lastComparison;
/*     */       }
/*     */     }
/* 570 */     lastComparison = Boolean.valueOf(isSetUserId()).compareTo(Boolean.valueOf(typedOther.isSetUserId()));
/* 571 */     if (lastComparison != 0) {
/* 572 */       return lastComparison;
/*     */     }
/* 574 */     if (isSetUserId()) {
/* 575 */       lastComparison = TBaseHelper.compareTo(this.userId, typedOther.userId);
/* 576 */       if (lastComparison != 0) {
/* 577 */         return lastComparison;
/*     */       }
/*     */     }
/* 580 */     lastComparison = Boolean.valueOf(isSetTenantName()).compareTo(Boolean.valueOf(typedOther.isSetTenantName()));
/* 581 */     if (lastComparison != 0) {
/* 582 */       return lastComparison;
/*     */     }
/* 584 */     if (isSetTenantName()) {
/* 585 */       lastComparison = TBaseHelper.compareTo(this.tenantName, typedOther.tenantName);
/* 586 */       if (lastComparison != 0) {
/* 587 */         return lastComparison;
/*     */       }
/*     */     }
/* 590 */     lastComparison = Boolean.valueOf(isSetSwiftAccountHash()).compareTo(Boolean.valueOf(typedOther.isSetSwiftAccountHash()));
/* 591 */     if (lastComparison != 0) {
/* 592 */       return lastComparison;
/*     */     }
/* 594 */     if (isSetSwiftAccountHash()) {
/* 595 */       lastComparison = TBaseHelper.compareTo(this.swiftAccountHash, typedOther.swiftAccountHash);
/* 596 */       if (lastComparison != 0) {
/* 597 */         return lastComparison;
/*     */       }
/*     */     }
/* 600 */     return 0;
/*     */   }
/*     */ 
/*     */   public _Fields fieldForId(int fieldId) {
/* 604 */     return _Fields.findByThriftId(fieldId);
/*     */   }
/*     */ 
/*     */   public void read(TProtocol iprot) throws TException {
/* 608 */     ((SchemeFactory)schemes.get(iprot.getScheme())).getScheme().read(iprot, this);
/*     */   }
/*     */ 
/*     */   public void write(TProtocol oprot) throws TException {
/* 612 */     ((SchemeFactory)schemes.get(oprot.getScheme())).getScheme().write(oprot, this);
/*     */   }
/*     */ 
/*     */   public String toString()
/*     */   {
/* 617 */     StringBuilder sb = new StringBuilder("User(");
/* 618 */     boolean first = true;
/*     */ 
/* 620 */     if (isSetUsername()) {
/* 621 */       sb.append("username:");
/* 622 */       if (this.username == null)
/* 623 */         sb.append("null");
/*     */       else {
/* 625 */         sb.append(this.username);
/*     */       }
/* 627 */       first = false;
/*     */     }
/* 629 */     if (isSetTenantId()) {
/* 630 */       if (!first) sb.append(", ");
/* 631 */       sb.append("tenantId:");
/* 632 */       if (this.tenantId == null)
/* 633 */         sb.append("null");
/*     */       else {
/* 635 */         sb.append(this.tenantId);
/*     */       }
/* 637 */       first = false;
/*     */     }
/* 639 */     if (isSetRoles()) {
/* 640 */       if (!first) sb.append(", ");
/* 641 */       sb.append("roles:");
/* 642 */       if (this.roles == null)
/* 643 */         sb.append("null");
/*     */       else {
/* 645 */         sb.append(this.roles);
/*     */       }
/* 647 */       first = false;
/*     */     }
/* 649 */     if (isSetUserId()) {
/* 650 */       if (!first) sb.append(", ");
/* 651 */       sb.append("userId:");
/* 652 */       if (this.userId == null)
/* 653 */         sb.append("null");
/*     */       else {
/* 655 */         sb.append(this.userId);
/*     */       }
/* 657 */       first = false;
/*     */     }
/* 659 */     if (isSetTenantName()) {
/* 660 */       if (!first) sb.append(", ");
/* 661 */       sb.append("tenantName:");
/* 662 */       if (this.tenantName == null)
/* 663 */         sb.append("null");
/*     */       else {
/* 665 */         sb.append(this.tenantName);
/*     */       }
/* 667 */       first = false;
/*     */     }
/* 669 */     if (isSetSwiftAccountHash()) {
/* 670 */       if (!first) sb.append(", ");
/* 671 */       sb.append("swiftAccountHash:");
/* 672 */       if (this.swiftAccountHash == null)
/* 673 */         sb.append("null");
/*     */       else {
/* 675 */         sb.append(this.swiftAccountHash);
/*     */       }
/* 677 */       first = false;
/*     */     }
/* 679 */     sb.append(")");
/* 680 */     return sb.toString();
/*     */   }
/*     */ 
/*     */   public void validate() throws TException
/*     */   {
/*     */   }
/*     */ 
/*     */   private void writeObject(ObjectOutputStream out) throws IOException {
/*     */     try {
/* 689 */       write(new TCompactProtocol(new TIOStreamTransport(out)));
/*     */     } catch (TException te) {
/* 691 */       throw new IOException(te);
/*     */     }
/*     */   }
/*     */ 
/*     */   private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
/*     */     try {
/* 697 */       read(new TCompactProtocol(new TIOStreamTransport(in)));
/*     */     } catch (TException te) {
/* 699 */       throw new IOException(te);
/*     */     }
/*     */   }
/*     */ 
/*     */   static
/*     */   {
/*  47 */     schemes.put(StandardScheme.class, new UserStandardSchemeFactory());
/*  48 */     schemes.put(TupleScheme.class, new UserTupleSchemeFactory());
/*     */ 
/* 135 */     Map tmpMap = new EnumMap(_Fields.class);
/* 136 */     tmpMap.put(_Fields.USERNAME, new FieldMetaData("username", (byte)2, new FieldValueMetaData((byte)11)));
/*     */ 
/* 138 */     tmpMap.put(_Fields.TENANT_ID, new FieldMetaData("tenantId", (byte)2, new FieldValueMetaData((byte)11)));
/*     */ 
/* 140 */     tmpMap.put(_Fields.ROLES, new FieldMetaData("roles", (byte)2, new ListMetaData((byte)15, new StructMetaData((byte)12, Role.class))));
/*     */ 
/* 143 */     tmpMap.put(_Fields.USER_ID, new FieldMetaData("userId", (byte)2, new FieldValueMetaData((byte)11)));
/*     */ 
/* 145 */     tmpMap.put(_Fields.TENANT_NAME, new FieldMetaData("tenantName", (byte)2, new FieldValueMetaData((byte)11)));
/*     */ 
/* 147 */     tmpMap.put(_Fields.SWIFT_ACCOUNT_HASH, new FieldMetaData("swiftAccountHash", (byte)2, new FieldValueMetaData((byte)11)));
/*     */ 
/* 149 */     metaDataMap = Collections.unmodifiableMap(tmpMap);
/* 150 */     FieldMetaData.addStructMetaDataMap(User.class, metaDataMap);
/*     */   }
/*     */ 
/*     */   private static class UserTupleScheme extends TupleScheme<User>
/*     */   {
/*     */     public void write(TProtocol prot, User struct)
/*     */       throws TException
/*     */     {
/* 860 */       TTupleProtocol oprot = (TTupleProtocol)prot;
/* 861 */       BitSet optionals = new BitSet();
/* 862 */       if (struct.isSetUsername()) {
/* 863 */         optionals.set(0);
/*     */       }
/* 865 */       if (struct.isSetTenantId()) {
/* 866 */         optionals.set(1);
/*     */       }
/* 868 */       if (struct.isSetRoles()) {
/* 869 */         optionals.set(2);
/*     */       }
/* 871 */       if (struct.isSetUserId()) {
/* 872 */         optionals.set(3);
/*     */       }
/* 874 */       if (struct.isSetTenantName()) {
/* 875 */         optionals.set(4);
/*     */       }
/* 877 */       if (struct.isSetSwiftAccountHash()) {
/* 878 */         optionals.set(5);
/*     */       }
/* 880 */       oprot.writeBitSet(optionals, 6);
/* 881 */       if (struct.isSetUsername()) {
/* 882 */         oprot.writeString(struct.username);
/*     */       }
/* 884 */       if (struct.isSetTenantId()) {
/* 885 */         oprot.writeString(struct.tenantId);
/*     */       }
/* 887 */       if (struct.isSetRoles())
/*     */       {
/* 889 */         oprot.writeI32(struct.roles.size());
/* 890 */         for (Role _iter4 : struct.roles)
/*     */         {
/* 892 */           _iter4.write(oprot);
/*     */         }
/*     */       }
/*     */ 
/* 896 */       if (struct.isSetUserId()) {
/* 897 */         oprot.writeString(struct.userId);
/*     */       }
/* 899 */       if (struct.isSetTenantName()) {
/* 900 */         oprot.writeString(struct.tenantName);
/*     */       }
/* 902 */       if (struct.isSetSwiftAccountHash())
/* 903 */         oprot.writeString(struct.swiftAccountHash);
/*     */     }
/*     */ 
/*     */     public void read(TProtocol prot, User struct)
/*     */       throws TException
/*     */     {
/* 909 */       TTupleProtocol iprot = (TTupleProtocol)prot;
/* 910 */       BitSet incoming = iprot.readBitSet(6);
/* 911 */       if (incoming.get(0)) {
/* 912 */         struct.username = iprot.readString();
/* 913 */         struct.setUsernameIsSet(true);
/*     */       }
/* 915 */       if (incoming.get(1)) {
/* 916 */         struct.tenantId = iprot.readString();
/* 917 */         struct.setTenantIdIsSet(true);
/*     */       }
/* 919 */       if (incoming.get(2))
/*     */       {
/* 921 */         TList _list5 = new TList((byte)12, iprot.readI32());
/* 922 */         struct.roles = new ArrayList(_list5.size);
/* 923 */         for (int _i6 = 0; _i6 < _list5.size; _i6++)
/*     */         {
/* 926 */           Role _elem7 = new Role();
/* 927 */           _elem7.read(iprot);
/* 928 */           struct.roles.add(_elem7);
/*     */         }
/*     */ 
/* 931 */         struct.setRolesIsSet(true);
/*     */       }
/* 933 */       if (incoming.get(3)) {
/* 934 */         struct.userId = iprot.readString();
/* 935 */         struct.setUserIdIsSet(true);
/*     */       }
/* 937 */       if (incoming.get(4)) {
/* 938 */         struct.tenantName = iprot.readString();
/* 939 */         struct.setTenantNameIsSet(true);
/*     */       }
/* 941 */       if (incoming.get(5)) {
/* 942 */         struct.swiftAccountHash = iprot.readString();
/* 943 */         struct.setSwiftAccountHashIsSet(true);
/*     */       }
/*     */     }
/*     */   }
/*     */ 
/*     */   private static class UserTupleSchemeFactory
/*     */     implements SchemeFactory
/*     */   {
/*     */     public User.UserTupleScheme getScheme()
/*     */     {
/* 852 */       return new User.UserTupleScheme();
/*     */     }
/*     */   }
/*     */ 
/*     */   private static class UserStandardScheme extends StandardScheme<User>
/*     */   {
/*     */     public void read(TProtocol iprot, User struct)
/*     */       throws TException
/*     */     {
/* 713 */       iprot.readStructBegin();
/*     */       while (true)
/*     */       {
/* 716 */         TField schemeField = iprot.readFieldBegin();
/* 717 */         if (schemeField.type == 0) {
/*     */           break;
/*     */         }
/* 720 */         switch (schemeField.id) {
/*     */         case 1:
/* 722 */           if (schemeField.type == 11) {
/* 723 */             struct.username = iprot.readString();
/* 724 */             struct.setUsernameIsSet(true);
/*     */           } else {
/* 726 */             TProtocolUtil.skip(iprot, schemeField.type);
/*     */           }
/* 728 */           break;
/*     */         case 2:
/* 730 */           if (schemeField.type == 11) {
/* 731 */             struct.tenantId = iprot.readString();
/* 732 */             struct.setTenantIdIsSet(true);
/*     */           } else {
/* 734 */             TProtocolUtil.skip(iprot, schemeField.type);
/*     */           }
/* 736 */           break;
/*     */         case 3:
/* 738 */           if (schemeField.type == 15)
/*     */           {
/* 740 */             TList _list0 = iprot.readListBegin();
/* 741 */             struct.roles = new ArrayList(_list0.size);
/* 742 */             for (int _i1 = 0; _i1 < _list0.size; _i1++)
/*     */             {
/* 745 */               Role _elem2 = new Role();
/* 746 */               _elem2.read(iprot);
/* 747 */               struct.roles.add(_elem2);
/*     */             }
/* 749 */             iprot.readListEnd();
/*     */ 
/* 751 */             struct.setRolesIsSet(true);
/*     */           } else {
/* 753 */             TProtocolUtil.skip(iprot, schemeField.type);
/*     */           }
/* 755 */           break;
/*     */         case 4:
/* 757 */           if (schemeField.type == 11) {
/* 758 */             struct.userId = iprot.readString();
/* 759 */             struct.setUserIdIsSet(true);
/*     */           } else {
/* 761 */             TProtocolUtil.skip(iprot, schemeField.type);
/*     */           }
/* 763 */           break;
/*     */         case 5:
/* 765 */           if (schemeField.type == 11) {
/* 766 */             struct.tenantName = iprot.readString();
/* 767 */             struct.setTenantNameIsSet(true);
/*     */           } else {
/* 769 */             TProtocolUtil.skip(iprot, schemeField.type);
/*     */           }
/* 771 */           break;
/*     */         case 6:
/* 773 */           if (schemeField.type == 11) {
/* 774 */             struct.swiftAccountHash = iprot.readString();
/* 775 */             struct.setSwiftAccountHashIsSet(true);
/*     */           } else {
/* 777 */             TProtocolUtil.skip(iprot, schemeField.type);
/*     */           }
/* 779 */           break;
/*     */         default:
/* 781 */           TProtocolUtil.skip(iprot, schemeField.type);
/*     */         }
/* 783 */         iprot.readFieldEnd();
/*     */       }
/* 785 */       iprot.readStructEnd();
/*     */ 
/* 788 */       struct.validate();
/*     */     }
/*     */ 
/*     */     public void write(TProtocol oprot, User struct) throws TException {
/* 792 */       struct.validate();
/*     */ 
/* 794 */       oprot.writeStructBegin(User.STRUCT_DESC);
/* 795 */       if ((struct.username != null) && 
/* 796 */         (struct.isSetUsername())) {
/* 797 */         oprot.writeFieldBegin(User.USERNAME_FIELD_DESC);
/* 798 */         oprot.writeString(struct.username);
/* 799 */         oprot.writeFieldEnd();
/*     */       }
/*     */ 
/* 802 */       if ((struct.tenantId != null) && 
/* 803 */         (struct.isSetTenantId())) {
/* 804 */         oprot.writeFieldBegin(User.TENANT_ID_FIELD_DESC);
/* 805 */         oprot.writeString(struct.tenantId);
/* 806 */         oprot.writeFieldEnd();
/*     */       }
/*     */ 
/* 809 */       if ((struct.roles != null) && 
/* 810 */         (struct.isSetRoles())) {
/* 811 */         oprot.writeFieldBegin(User.ROLES_FIELD_DESC);
/*     */ 
/* 813 */         oprot.writeListBegin(new TList((byte)12, struct.roles.size()));
/* 814 */         for (Role _iter3 : struct.roles)
/*     */         {
/* 816 */           _iter3.write(oprot);
/*     */         }
/* 818 */         oprot.writeListEnd();
/*     */ 
/* 820 */         oprot.writeFieldEnd();
/*     */       }
/*     */ 
/* 823 */       if ((struct.userId != null) && 
/* 824 */         (struct.isSetUserId())) {
/* 825 */         oprot.writeFieldBegin(User.USER_ID_FIELD_DESC);
/* 826 */         oprot.writeString(struct.userId);
/* 827 */         oprot.writeFieldEnd();
/*     */       }
/*     */ 
/* 830 */       if ((struct.tenantName != null) && 
/* 831 */         (struct.isSetTenantName())) {
/* 832 */         oprot.writeFieldBegin(User.TENANT_NAME_FIELD_DESC);
/* 833 */         oprot.writeString(struct.tenantName);
/* 834 */         oprot.writeFieldEnd();
/*     */       }
/*     */ 
/* 837 */       if ((struct.swiftAccountHash != null) && 
/* 838 */         (struct.isSetSwiftAccountHash())) {
/* 839 */         oprot.writeFieldBegin(User.SWIFT_ACCOUNT_HASH_FIELD_DESC);
/* 840 */         oprot.writeString(struct.swiftAccountHash);
/* 841 */         oprot.writeFieldEnd();
/*     */       }
/*     */ 
/* 844 */       oprot.writeFieldStop();
/* 845 */       oprot.writeStructEnd();
/*     */     }
/*     */   }
/*     */ 
/*     */   private static class UserStandardSchemeFactory
/*     */     implements SchemeFactory
/*     */   {
/*     */     public User.UserStandardScheme getScheme()
/*     */     {
/* 705 */       return new User.UserStandardScheme();
/*     */     }
/*     */   }
/*     */ 
/*     */   public static enum _Fields
/*     */     implements TFieldIdEnum
/*     */   {
/*  60 */     USERNAME((short)1, "username"), 
/*  61 */     TENANT_ID((short)2, "tenantId"), 
/*  62 */     ROLES((short)3, "roles"), 
/*  63 */     USER_ID((short)4, "userId"), 
/*  64 */     TENANT_NAME((short)5, "tenantName"), 
/*  65 */     SWIFT_ACCOUNT_HASH((short)6, "swiftAccountHash");
/*     */ 
/*     */     private static final Map<String, _Fields> byName;
/*     */     private final short _thriftId;
/*     */     private final String _fieldName;
/*     */ 
/*     */     public static _Fields findByThriftId(int fieldId)
/*     */     {
/*  79 */       switch (fieldId) {
/*     */       case 1:
/*  81 */         return USERNAME;
/*     */       case 2:
/*  83 */         return TENANT_ID;
/*     */       case 3:
/*  85 */         return ROLES;
/*     */       case 4:
/*  87 */         return USER_ID;
/*     */       case 5:
/*  89 */         return TENANT_NAME;
/*     */       case 6:
/*  91 */         return SWIFT_ACCOUNT_HASH;
/*     */       }
/*  93 */       return null;
/*     */     }
/*     */ 
/*     */     public static _Fields findByThriftIdOrThrow(int fieldId)
/*     */     {
/* 102 */       _Fields fields = findByThriftId(fieldId);
/* 103 */       if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
/* 104 */       return fields;
/*     */     }
/*     */ 
/*     */     public static _Fields findByName(String name)
/*     */     {
/* 111 */       return (_Fields)byName.get(name);
/*     */     }
/*     */ 
/*     */     private _Fields(short thriftId, String fieldName)
/*     */     {
/* 118 */       this._thriftId = thriftId;
/* 119 */       this._fieldName = fieldName;
/*     */     }
/*     */ 
/*     */     public short getThriftFieldId() {
/* 123 */       return this._thriftId;
/*     */     }
/*     */ 
/*     */     public String getFieldName() {
/* 127 */       return this._fieldName;
/*     */     }
/*     */ 
/*     */     static
/*     */     {
/*  67 */       byName = new HashMap();
/*     */ 
/*  70 */       for (_Fields field : EnumSet.allOf(_Fields.class))
/*  71 */         byName.put(field.getFieldName(), field);
/*     */     }
/*     */   }
/*     */ }

/* Location:           /Users/johnderr/.m2/repository/com/hp/csbu/cc/CsThriftModel/1.2-SNAPSHOT/CsThriftModel-1.2-20140130.160951-1223.jar
 * Qualified Name:     com.hp.csbu.cc.security.cs.thrift.service.User
 * JD-Core Version:    0.6.2
 */