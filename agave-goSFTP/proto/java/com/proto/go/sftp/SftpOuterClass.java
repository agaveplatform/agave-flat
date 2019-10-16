// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: sftp.proto

package com.proto.go.sftp;

public final class SftpOuterClass {
  private SftpOuterClass() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_sftp_Sftp_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_sftp_Sftp_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_sftp_CopyLocalToRemoteRequest_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_sftp_CopyLocalToRemoteRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_sftp_CopyLocalToRemoteResponse_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_sftp_CopyLocalToRemoteResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_sftp_CopyFromRemoteRequest_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_sftp_CopyFromRemoteRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_sftp_CopyFromRemoteResponse_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_sftp_CopyFromRemoteResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_sftp_List_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_sftp_List_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_sftp_GetDirRequest_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_sftp_GetDirRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_sftp_GetDirResponse_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_sftp_GetDirResponse_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\nsftp.proto\022\004sftp\"\204\001\n\004Sftp\022\020\n\010username\030" +
      "\001 \001(\t\022\020\n\010passWord\030\002 \001(\t\022\020\n\010systemId\030\003 \001(" +
      "\t\022\017\n\007hostKey\030\004 \001(\t\022\020\n\010fileName\030\005 \001(\t\022\020\n\010" +
      "hostPort\030\006 \001(\t\022\021\n\tclientKey\030\007 \001(\t\"4\n\030Cop" +
      "yLocalToRemoteRequest\022\030\n\004sftp\030\001 \001(\0132\n.sf" +
      "tp.Sftp\"+\n\031CopyLocalToRemoteResponse\022\016\n\006" +
      "result\030\001 \001(\t\"1\n\025CopyFromRemoteRequest\022\030\n" +
      "\004sftp\030\001 \001(\0132\n.sftp.Sftp\"(\n\026CopyFromRemot" +
      "eResponse\022\016\n\006result\030\001 \001(\t\"\244\001\n\004List\022\020\n\010fi" +
      "leName\030\001 \001(\t\022\017\n\007fileDir\030\002 \001(\t\022\020\n\010fileSiz" +
      "e\030\003 \001(\t\022\r\n\005isDir\030\004 \001(\t\022\020\n\010startDir\030\005 \001(\t" +
      "\022\020\n\010userName\030\006 \001(\t\022\020\n\010passWord\030\007 \001(\t\022\020\n\010" +
      "systemId\030\010 \001(\t\022\020\n\010hostPort\030\t \001(\t\")\n\rGetD" +
      "irRequest\022\030\n\004list\030\001 \001(\0132\n.sftp.List\" \n\016G" +
      "etDirResponse\022\016\n\006result\030\001 \001(\t2\365\001\n\004SFTP\022R" +
      "\n\025CopyFromRemoteService\022\033.sftp.CopyFromR" +
      "emoteRequest\032\034.sftp.CopyFromRemoteRespon" +
      "se\022[\n\030CopyLocalToRemoteService\022\036.sftp.Co" +
      "pyLocalToRemoteRequest\032\037.sftp.CopyLocalT" +
      "oRemoteResponse\022<\n\rGetDirListing\022\023.sftp." +
      "GetDirRequest\032\024.sftp.GetDirResponse0\001B\033\n" +
      "\021com.proto.go.sftpP\001Z\004sftpb\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        });
    internal_static_sftp_Sftp_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_sftp_Sftp_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_sftp_Sftp_descriptor,
        new java.lang.String[] { "Username", "PassWord", "SystemId", "HostKey", "FileName", "HostPort", "ClientKey", });
    internal_static_sftp_CopyLocalToRemoteRequest_descriptor =
      getDescriptor().getMessageTypes().get(1);
    internal_static_sftp_CopyLocalToRemoteRequest_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_sftp_CopyLocalToRemoteRequest_descriptor,
        new java.lang.String[] { "Sftp", });
    internal_static_sftp_CopyLocalToRemoteResponse_descriptor =
      getDescriptor().getMessageTypes().get(2);
    internal_static_sftp_CopyLocalToRemoteResponse_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_sftp_CopyLocalToRemoteResponse_descriptor,
        new java.lang.String[] { "Result", });
    internal_static_sftp_CopyFromRemoteRequest_descriptor =
      getDescriptor().getMessageTypes().get(3);
    internal_static_sftp_CopyFromRemoteRequest_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_sftp_CopyFromRemoteRequest_descriptor,
        new java.lang.String[] { "Sftp", });
    internal_static_sftp_CopyFromRemoteResponse_descriptor =
      getDescriptor().getMessageTypes().get(4);
    internal_static_sftp_CopyFromRemoteResponse_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_sftp_CopyFromRemoteResponse_descriptor,
        new java.lang.String[] { "Result", });
    internal_static_sftp_List_descriptor =
      getDescriptor().getMessageTypes().get(5);
    internal_static_sftp_List_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_sftp_List_descriptor,
        new java.lang.String[] { "FileName", "FileDir", "FileSize", "IsDir", "StartDir", "UserName", "PassWord", "SystemId", "HostPort", });
    internal_static_sftp_GetDirRequest_descriptor =
      getDescriptor().getMessageTypes().get(6);
    internal_static_sftp_GetDirRequest_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_sftp_GetDirRequest_descriptor,
        new java.lang.String[] { "List", });
    internal_static_sftp_GetDirResponse_descriptor =
      getDescriptor().getMessageTypes().get(7);
    internal_static_sftp_GetDirResponse_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_sftp_GetDirResponse_descriptor,
        new java.lang.String[] { "Result", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
