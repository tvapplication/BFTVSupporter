/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: D:\\¿ª·¢\\workspace2\\com.egame.tv.v5_545\\src\\com\\egame\\tv\\services\\aidl\\IEgameService.aidl
 */
package com.egame.tv.services.aidl;
public interface IEgameService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.egame.tv.services.aidl.IEgameService
{
private static final java.lang.String DESCRIPTOR = "com.egame.tv.services.aidl.IEgameService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an com.egame.tv.services.aidl.IEgameService interface,
 * generating a proxy if needed.
 */
public static com.egame.tv.services.aidl.IEgameService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof com.egame.tv.services.aidl.IEgameService))) {
return ((com.egame.tv.services.aidl.IEgameService)iin);
}
return new com.egame.tv.services.aidl.IEgameService.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_getValue:
{
data.enforceInterface(DESCRIPTOR);
java.util.List<com.egame.tv.services.aidl.EgameInstallAppBean> _result = this.getValue();
reply.writeNoException();
reply.writeTypedList(_result);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.egame.tv.services.aidl.IEgameService
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public java.util.List<com.egame.tv.services.aidl.EgameInstallAppBean> getValue() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.List<com.egame.tv.services.aidl.EgameInstallAppBean> _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getValue, _data, _reply, 0);
_reply.readException();
_result = _reply.createTypedArrayList(com.egame.tv.services.aidl.EgameInstallAppBean.CREATOR);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_getValue = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
}
public java.util.List<com.egame.tv.services.aidl.EgameInstallAppBean> getValue() throws android.os.RemoteException;
}
