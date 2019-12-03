// Code generated by protoc-gen-go. DO NOT EDIT.
// source: sftp.proto

package sftpproto

import (
	context "context"
	fmt "fmt"
	proto "github.com/golang/protobuf/proto"
	grpc "google.golang.org/grpc"
	codes "google.golang.org/grpc/codes"
	status "google.golang.org/grpc/status"
	math "math"
)

// Reference imports to suppress errors if they are not otherwise used.
var _ = proto.Marshal
var _ = fmt.Errorf
var _ = math.Inf

// This is a compile-time assertion to ensure that this generated file
// is compatible with the proto package it is being compiled against.
// A compilation error at this line likely means your copy of the
// proto package needs to be updated.
const _ = proto.ProtoPackageIsVersion3 // please upgrade the proto package

type Sftp struct {
	Username             string   `protobuf:"bytes,1,opt,name=username,proto3" json:"username,omitempty"`
	PassWord             string   `protobuf:"bytes,2,opt,name=passWord,proto3" json:"passWord,omitempty"`
	SystemId             string   `protobuf:"bytes,3,opt,name=systemId,proto3" json:"systemId,omitempty"`
	HostKey              string   `protobuf:"bytes,4,opt,name=hostKey,proto3" json:"hostKey,omitempty"`
	FileName             string   `protobuf:"bytes,5,opt,name=fileName,proto3" json:"fileName,omitempty"`
	FileSize             int64    `protobuf:"varint,6,opt,name=fileSize,proto3" json:"fileSize,omitempty"`
	HostPort             string   `protobuf:"bytes,7,opt,name=hostPort,proto3" json:"hostPort,omitempty"`
	ClientKey            string   `protobuf:"bytes,8,opt,name=clientKey,proto3" json:"clientKey,omitempty"`
	BufferSize           int64    `protobuf:"varint,9,opt,name=bufferSize,proto3" json:"bufferSize,omitempty"`
	Type                 string   `protobuf:"bytes,10,opt,name=type,proto3" json:"type,omitempty"`
	DestFileName         string   `protobuf:"bytes,11,opt,name=destFileName,proto3" json:"destFileName,omitempty"`
	XXX_NoUnkeyedLiteral struct{} `json:"-"`
	XXX_unrecognized     []byte   `json:"-"`
	XXX_sizecache        int32    `json:"-"`
}

func (m *Sftp) Reset()         { *m = Sftp{} }
func (m *Sftp) String() string { return proto.CompactTextString(m) }
func (*Sftp) ProtoMessage()    {}
func (*Sftp) Descriptor() ([]byte, []int) {
	return fileDescriptor_78fc9c5a618b7cda, []int{0}
}

func (m *Sftp) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_Sftp.Unmarshal(m, b)
}
func (m *Sftp) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_Sftp.Marshal(b, m, deterministic)
}
func (m *Sftp) XXX_Merge(src proto.Message) {
	xxx_messageInfo_Sftp.Merge(m, src)
}
func (m *Sftp) XXX_Size() int {
	return xxx_messageInfo_Sftp.Size(m)
}
func (m *Sftp) XXX_DiscardUnknown() {
	xxx_messageInfo_Sftp.DiscardUnknown(m)
}

var xxx_messageInfo_Sftp proto.InternalMessageInfo

func (m *Sftp) GetUsername() string {
	if m != nil {
		return m.Username
	}
	return ""
}

func (m *Sftp) GetPassWord() string {
	if m != nil {
		return m.PassWord
	}
	return ""
}

func (m *Sftp) GetSystemId() string {
	if m != nil {
		return m.SystemId
	}
	return ""
}

func (m *Sftp) GetHostKey() string {
	if m != nil {
		return m.HostKey
	}
	return ""
}

func (m *Sftp) GetFileName() string {
	if m != nil {
		return m.FileName
	}
	return ""
}

func (m *Sftp) GetFileSize() int64 {
	if m != nil {
		return m.FileSize
	}
	return 0
}

func (m *Sftp) GetHostPort() string {
	if m != nil {
		return m.HostPort
	}
	return ""
}

func (m *Sftp) GetClientKey() string {
	if m != nil {
		return m.ClientKey
	}
	return ""
}

func (m *Sftp) GetBufferSize() int64 {
	if m != nil {
		return m.BufferSize
	}
	return 0
}

func (m *Sftp) GetType() string {
	if m != nil {
		return m.Type
	}
	return ""
}

func (m *Sftp) GetDestFileName() string {
	if m != nil {
		return m.DestFileName
	}
	return ""
}

type SrvPutRequest struct {
	SrceSftp             *Sftp    `protobuf:"bytes,1,opt,name=srceSftp,proto3" json:"srceSftp,omitempty"`
	XXX_NoUnkeyedLiteral struct{} `json:"-"`
	XXX_unrecognized     []byte   `json:"-"`
	XXX_sizecache        int32    `json:"-"`
}

func (m *SrvPutRequest) Reset()         { *m = SrvPutRequest{} }
func (m *SrvPutRequest) String() string { return proto.CompactTextString(m) }
func (*SrvPutRequest) ProtoMessage()    {}
func (*SrvPutRequest) Descriptor() ([]byte, []int) {
	return fileDescriptor_78fc9c5a618b7cda, []int{1}
}

func (m *SrvPutRequest) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_SrvPutRequest.Unmarshal(m, b)
}
func (m *SrvPutRequest) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_SrvPutRequest.Marshal(b, m, deterministic)
}
func (m *SrvPutRequest) XXX_Merge(src proto.Message) {
	xxx_messageInfo_SrvPutRequest.Merge(m, src)
}
func (m *SrvPutRequest) XXX_Size() int {
	return xxx_messageInfo_SrvPutRequest.Size(m)
}
func (m *SrvPutRequest) XXX_DiscardUnknown() {
	xxx_messageInfo_SrvPutRequest.DiscardUnknown(m)
}

var xxx_messageInfo_SrvPutRequest proto.InternalMessageInfo

func (m *SrvPutRequest) GetSrceSftp() *Sftp {
	if m != nil {
		return m.SrceSftp
	}
	return nil
}

type SrvPutResponse struct {
	FileName             string   `protobuf:"bytes,1,opt,name=fileName,proto3" json:"fileName,omitempty"`
	BytesReturned        string   `protobuf:"bytes,2,opt,name=bytesReturned,proto3" json:"bytesReturned,omitempty"`
	Error                string   `protobuf:"bytes,3,opt,name=error,proto3" json:"error,omitempty"`
	XXX_NoUnkeyedLiteral struct{} `json:"-"`
	XXX_unrecognized     []byte   `json:"-"`
	XXX_sizecache        int32    `json:"-"`
}

func (m *SrvPutResponse) Reset()         { *m = SrvPutResponse{} }
func (m *SrvPutResponse) String() string { return proto.CompactTextString(m) }
func (*SrvPutResponse) ProtoMessage()    {}
func (*SrvPutResponse) Descriptor() ([]byte, []int) {
	return fileDescriptor_78fc9c5a618b7cda, []int{2}
}

func (m *SrvPutResponse) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_SrvPutResponse.Unmarshal(m, b)
}
func (m *SrvPutResponse) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_SrvPutResponse.Marshal(b, m, deterministic)
}
func (m *SrvPutResponse) XXX_Merge(src proto.Message) {
	xxx_messageInfo_SrvPutResponse.Merge(m, src)
}
func (m *SrvPutResponse) XXX_Size() int {
	return xxx_messageInfo_SrvPutResponse.Size(m)
}
func (m *SrvPutResponse) XXX_DiscardUnknown() {
	xxx_messageInfo_SrvPutResponse.DiscardUnknown(m)
}

var xxx_messageInfo_SrvPutResponse proto.InternalMessageInfo

func (m *SrvPutResponse) GetFileName() string {
	if m != nil {
		return m.FileName
	}
	return ""
}

func (m *SrvPutResponse) GetBytesReturned() string {
	if m != nil {
		return m.BytesReturned
	}
	return ""
}

func (m *SrvPutResponse) GetError() string {
	if m != nil {
		return m.Error
	}
	return ""
}

type SrvGetRequest struct {
	SrceSftp             *Sftp    `protobuf:"bytes,1,opt,name=srceSftp,proto3" json:"srceSftp,omitempty"`
	XXX_NoUnkeyedLiteral struct{} `json:"-"`
	XXX_unrecognized     []byte   `json:"-"`
	XXX_sizecache        int32    `json:"-"`
}

func (m *SrvGetRequest) Reset()         { *m = SrvGetRequest{} }
func (m *SrvGetRequest) String() string { return proto.CompactTextString(m) }
func (*SrvGetRequest) ProtoMessage()    {}
func (*SrvGetRequest) Descriptor() ([]byte, []int) {
	return fileDescriptor_78fc9c5a618b7cda, []int{3}
}

func (m *SrvGetRequest) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_SrvGetRequest.Unmarshal(m, b)
}
func (m *SrvGetRequest) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_SrvGetRequest.Marshal(b, m, deterministic)
}
func (m *SrvGetRequest) XXX_Merge(src proto.Message) {
	xxx_messageInfo_SrvGetRequest.Merge(m, src)
}
func (m *SrvGetRequest) XXX_Size() int {
	return xxx_messageInfo_SrvGetRequest.Size(m)
}
func (m *SrvGetRequest) XXX_DiscardUnknown() {
	xxx_messageInfo_SrvGetRequest.DiscardUnknown(m)
}

var xxx_messageInfo_SrvGetRequest proto.InternalMessageInfo

func (m *SrvGetRequest) GetSrceSftp() *Sftp {
	if m != nil {
		return m.SrceSftp
	}
	return nil
}

type SrvGetResponse struct {
	FileName             string   `protobuf:"bytes,1,opt,name=fileName,proto3" json:"fileName,omitempty"`
	BytesReturned        string   `protobuf:"bytes,2,opt,name=bytesReturned,proto3" json:"bytesReturned,omitempty"`
	Error                string   `protobuf:"bytes,3,opt,name=error,proto3" json:"error,omitempty"`
	XXX_NoUnkeyedLiteral struct{} `json:"-"`
	XXX_unrecognized     []byte   `json:"-"`
	XXX_sizecache        int32    `json:"-"`
}

func (m *SrvGetResponse) Reset()         { *m = SrvGetResponse{} }
func (m *SrvGetResponse) String() string { return proto.CompactTextString(m) }
func (*SrvGetResponse) ProtoMessage()    {}
func (*SrvGetResponse) Descriptor() ([]byte, []int) {
	return fileDescriptor_78fc9c5a618b7cda, []int{4}
}

func (m *SrvGetResponse) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_SrvGetResponse.Unmarshal(m, b)
}
func (m *SrvGetResponse) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_SrvGetResponse.Marshal(b, m, deterministic)
}
func (m *SrvGetResponse) XXX_Merge(src proto.Message) {
	xxx_messageInfo_SrvGetResponse.Merge(m, src)
}
func (m *SrvGetResponse) XXX_Size() int {
	return xxx_messageInfo_SrvGetResponse.Size(m)
}
func (m *SrvGetResponse) XXX_DiscardUnknown() {
	xxx_messageInfo_SrvGetResponse.DiscardUnknown(m)
}

var xxx_messageInfo_SrvGetResponse proto.InternalMessageInfo

func (m *SrvGetResponse) GetFileName() string {
	if m != nil {
		return m.FileName
	}
	return ""
}

func (m *SrvGetResponse) GetBytesReturned() string {
	if m != nil {
		return m.BytesReturned
	}
	return ""
}

func (m *SrvGetResponse) GetError() string {
	if m != nil {
		return m.Error
	}
	return ""
}

type SrvMkdirsRequest struct {
	SrceSftp             *Sftp    `protobuf:"bytes,1,opt,name=srceSftp,proto3" json:"srceSftp,omitempty"`
	XXX_NoUnkeyedLiteral struct{} `json:"-"`
	XXX_unrecognized     []byte   `json:"-"`
	XXX_sizecache        int32    `json:"-"`
}

func (m *SrvMkdirsRequest) Reset()         { *m = SrvMkdirsRequest{} }
func (m *SrvMkdirsRequest) String() string { return proto.CompactTextString(m) }
func (*SrvMkdirsRequest) ProtoMessage()    {}
func (*SrvMkdirsRequest) Descriptor() ([]byte, []int) {
	return fileDescriptor_78fc9c5a618b7cda, []int{5}
}

func (m *SrvMkdirsRequest) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_SrvMkdirsRequest.Unmarshal(m, b)
}
func (m *SrvMkdirsRequest) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_SrvMkdirsRequest.Marshal(b, m, deterministic)
}
func (m *SrvMkdirsRequest) XXX_Merge(src proto.Message) {
	xxx_messageInfo_SrvMkdirsRequest.Merge(m, src)
}
func (m *SrvMkdirsRequest) XXX_Size() int {
	return xxx_messageInfo_SrvMkdirsRequest.Size(m)
}
func (m *SrvMkdirsRequest) XXX_DiscardUnknown() {
	xxx_messageInfo_SrvMkdirsRequest.DiscardUnknown(m)
}

var xxx_messageInfo_SrvMkdirsRequest proto.InternalMessageInfo

func (m *SrvMkdirsRequest) GetSrceSftp() *Sftp {
	if m != nil {
		return m.SrceSftp
	}
	return nil
}

type SrvMkdirsResponse struct {
	FileName             string   `protobuf:"bytes,1,opt,name=fileName,proto3" json:"fileName,omitempty"`
	Error                string   `protobuf:"bytes,2,opt,name=error,proto3" json:"error,omitempty"`
	XXX_NoUnkeyedLiteral struct{} `json:"-"`
	XXX_unrecognized     []byte   `json:"-"`
	XXX_sizecache        int32    `json:"-"`
}

func (m *SrvMkdirsResponse) Reset()         { *m = SrvMkdirsResponse{} }
func (m *SrvMkdirsResponse) String() string { return proto.CompactTextString(m) }
func (*SrvMkdirsResponse) ProtoMessage()    {}
func (*SrvMkdirsResponse) Descriptor() ([]byte, []int) {
	return fileDescriptor_78fc9c5a618b7cda, []int{6}
}

func (m *SrvMkdirsResponse) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_SrvMkdirsResponse.Unmarshal(m, b)
}
func (m *SrvMkdirsResponse) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_SrvMkdirsResponse.Marshal(b, m, deterministic)
}
func (m *SrvMkdirsResponse) XXX_Merge(src proto.Message) {
	xxx_messageInfo_SrvMkdirsResponse.Merge(m, src)
}
func (m *SrvMkdirsResponse) XXX_Size() int {
	return xxx_messageInfo_SrvMkdirsResponse.Size(m)
}
func (m *SrvMkdirsResponse) XXX_DiscardUnknown() {
	xxx_messageInfo_SrvMkdirsResponse.DiscardUnknown(m)
}

var xxx_messageInfo_SrvMkdirsResponse proto.InternalMessageInfo

func (m *SrvMkdirsResponse) GetFileName() string {
	if m != nil {
		return m.FileName
	}
	return ""
}

func (m *SrvMkdirsResponse) GetError() string {
	if m != nil {
		return m.Error
	}
	return ""
}

type SrvRemoveRequest struct {
	SrceSftp             *Sftp    `protobuf:"bytes,1,opt,name=srceSftp,proto3" json:"srceSftp,omitempty"`
	XXX_NoUnkeyedLiteral struct{} `json:"-"`
	XXX_unrecognized     []byte   `json:"-"`
	XXX_sizecache        int32    `json:"-"`
}

func (m *SrvRemoveRequest) Reset()         { *m = SrvRemoveRequest{} }
func (m *SrvRemoveRequest) String() string { return proto.CompactTextString(m) }
func (*SrvRemoveRequest) ProtoMessage()    {}
func (*SrvRemoveRequest) Descriptor() ([]byte, []int) {
	return fileDescriptor_78fc9c5a618b7cda, []int{7}
}

func (m *SrvRemoveRequest) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_SrvRemoveRequest.Unmarshal(m, b)
}
func (m *SrvRemoveRequest) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_SrvRemoveRequest.Marshal(b, m, deterministic)
}
func (m *SrvRemoveRequest) XXX_Merge(src proto.Message) {
	xxx_messageInfo_SrvRemoveRequest.Merge(m, src)
}
func (m *SrvRemoveRequest) XXX_Size() int {
	return xxx_messageInfo_SrvRemoveRequest.Size(m)
}
func (m *SrvRemoveRequest) XXX_DiscardUnknown() {
	xxx_messageInfo_SrvRemoveRequest.DiscardUnknown(m)
}

var xxx_messageInfo_SrvRemoveRequest proto.InternalMessageInfo

func (m *SrvRemoveRequest) GetSrceSftp() *Sftp {
	if m != nil {
		return m.SrceSftp
	}
	return nil
}

type SrvRemoveResponse struct {
	FileName             string   `protobuf:"bytes,1,opt,name=fileName,proto3" json:"fileName,omitempty"`
	Error                string   `protobuf:"bytes,2,opt,name=error,proto3" json:"error,omitempty"`
	XXX_NoUnkeyedLiteral struct{} `json:"-"`
	XXX_unrecognized     []byte   `json:"-"`
	XXX_sizecache        int32    `json:"-"`
}

func (m *SrvRemoveResponse) Reset()         { *m = SrvRemoveResponse{} }
func (m *SrvRemoveResponse) String() string { return proto.CompactTextString(m) }
func (*SrvRemoveResponse) ProtoMessage()    {}
func (*SrvRemoveResponse) Descriptor() ([]byte, []int) {
	return fileDescriptor_78fc9c5a618b7cda, []int{8}
}

func (m *SrvRemoveResponse) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_SrvRemoveResponse.Unmarshal(m, b)
}
func (m *SrvRemoveResponse) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_SrvRemoveResponse.Marshal(b, m, deterministic)
}
func (m *SrvRemoveResponse) XXX_Merge(src proto.Message) {
	xxx_messageInfo_SrvRemoveResponse.Merge(m, src)
}
func (m *SrvRemoveResponse) XXX_Size() int {
	return xxx_messageInfo_SrvRemoveResponse.Size(m)
}
func (m *SrvRemoveResponse) XXX_DiscardUnknown() {
	xxx_messageInfo_SrvRemoveResponse.DiscardUnknown(m)
}

var xxx_messageInfo_SrvRemoveResponse proto.InternalMessageInfo

func (m *SrvRemoveResponse) GetFileName() string {
	if m != nil {
		return m.FileName
	}
	return ""
}

func (m *SrvRemoveResponse) GetError() string {
	if m != nil {
		return m.Error
	}
	return ""
}

type AuthenticateToRemoteRequest struct {
	Auth                 *Sftp    `protobuf:"bytes,1,opt,name=auth,proto3" json:"auth,omitempty"`
	XXX_NoUnkeyedLiteral struct{} `json:"-"`
	XXX_unrecognized     []byte   `json:"-"`
	XXX_sizecache        int32    `json:"-"`
}

func (m *AuthenticateToRemoteRequest) Reset()         { *m = AuthenticateToRemoteRequest{} }
func (m *AuthenticateToRemoteRequest) String() string { return proto.CompactTextString(m) }
func (*AuthenticateToRemoteRequest) ProtoMessage()    {}
func (*AuthenticateToRemoteRequest) Descriptor() ([]byte, []int) {
	return fileDescriptor_78fc9c5a618b7cda, []int{9}
}

func (m *AuthenticateToRemoteRequest) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_AuthenticateToRemoteRequest.Unmarshal(m, b)
}
func (m *AuthenticateToRemoteRequest) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_AuthenticateToRemoteRequest.Marshal(b, m, deterministic)
}
func (m *AuthenticateToRemoteRequest) XXX_Merge(src proto.Message) {
	xxx_messageInfo_AuthenticateToRemoteRequest.Merge(m, src)
}
func (m *AuthenticateToRemoteRequest) XXX_Size() int {
	return xxx_messageInfo_AuthenticateToRemoteRequest.Size(m)
}
func (m *AuthenticateToRemoteRequest) XXX_DiscardUnknown() {
	xxx_messageInfo_AuthenticateToRemoteRequest.DiscardUnknown(m)
}

var xxx_messageInfo_AuthenticateToRemoteRequest proto.InternalMessageInfo

func (m *AuthenticateToRemoteRequest) GetAuth() *Sftp {
	if m != nil {
		return m.Auth
	}
	return nil
}

type AuthenticateToRemoteResponse struct {
	Response             string   `protobuf:"bytes,1,opt,name=response,proto3" json:"response,omitempty"`
	XXX_NoUnkeyedLiteral struct{} `json:"-"`
	XXX_unrecognized     []byte   `json:"-"`
	XXX_sizecache        int32    `json:"-"`
}

func (m *AuthenticateToRemoteResponse) Reset()         { *m = AuthenticateToRemoteResponse{} }
func (m *AuthenticateToRemoteResponse) String() string { return proto.CompactTextString(m) }
func (*AuthenticateToRemoteResponse) ProtoMessage()    {}
func (*AuthenticateToRemoteResponse) Descriptor() ([]byte, []int) {
	return fileDescriptor_78fc9c5a618b7cda, []int{10}
}

func (m *AuthenticateToRemoteResponse) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_AuthenticateToRemoteResponse.Unmarshal(m, b)
}
func (m *AuthenticateToRemoteResponse) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_AuthenticateToRemoteResponse.Marshal(b, m, deterministic)
}
func (m *AuthenticateToRemoteResponse) XXX_Merge(src proto.Message) {
	xxx_messageInfo_AuthenticateToRemoteResponse.Merge(m, src)
}
func (m *AuthenticateToRemoteResponse) XXX_Size() int {
	return xxx_messageInfo_AuthenticateToRemoteResponse.Size(m)
}
func (m *AuthenticateToRemoteResponse) XXX_DiscardUnknown() {
	xxx_messageInfo_AuthenticateToRemoteResponse.DiscardUnknown(m)
}

var xxx_messageInfo_AuthenticateToRemoteResponse proto.InternalMessageInfo

func (m *AuthenticateToRemoteResponse) GetResponse() string {
	if m != nil {
		return m.Response
	}
	return ""
}

func init() {
	proto.RegisterType((*Sftp)(nil), "sftpproto.Sftp")
	proto.RegisterType((*SrvPutRequest)(nil), "sftpproto.SrvPutRequest")
	proto.RegisterType((*SrvPutResponse)(nil), "sftpproto.SrvPutResponse")
	proto.RegisterType((*SrvGetRequest)(nil), "sftpproto.SrvGetRequest")
	proto.RegisterType((*SrvGetResponse)(nil), "sftpproto.SrvGetResponse")
	proto.RegisterType((*SrvMkdirsRequest)(nil), "sftpproto.SrvMkdirsRequest")
	proto.RegisterType((*SrvMkdirsResponse)(nil), "sftpproto.SrvMkdirsResponse")
	proto.RegisterType((*SrvRemoveRequest)(nil), "sftpproto.SrvRemoveRequest")
	proto.RegisterType((*SrvRemoveResponse)(nil), "sftpproto.SrvRemoveResponse")
	proto.RegisterType((*AuthenticateToRemoteRequest)(nil), "sftpproto.AuthenticateToRemoteRequest")
	proto.RegisterType((*AuthenticateToRemoteResponse)(nil), "sftpproto.AuthenticateToRemoteResponse")
}

func init() { proto.RegisterFile("sftp.proto", fileDescriptor_78fc9c5a618b7cda) }

var fileDescriptor_78fc9c5a618b7cda = []byte{
	// 531 bytes of a gzipped FileDescriptorProto
	0x1f, 0x8b, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0xff, 0xb4, 0x54, 0xdd, 0x8a, 0xd3, 0x50,
	0x10, 0xa6, 0x69, 0x77, 0xb7, 0x99, 0xfd, 0x51, 0x0f, 0x5e, 0xc4, 0x6e, 0x91, 0x25, 0xfe, 0x2d,
	0x08, 0xb9, 0x58, 0xef, 0x8a, 0x20, 0x5b, 0xd1, 0x45, 0x44, 0x09, 0xa9, 0x20, 0x78, 0x23, 0x69,
	0x3b, 0xd9, 0x06, 0xdb, 0x9c, 0x78, 0xce, 0xa4, 0x10, 0x1f, 0xc7, 0x07, 0xf1, 0x91, 0x7c, 0x06,
	0x39, 0x27, 0x3d, 0xf9, 0xdb, 0xfa, 0xc3, 0x2e, 0xde, 0xcd, 0x37, 0xdf, 0xcc, 0x37, 0x1f, 0x33,
	0xc9, 0x01, 0x90, 0x11, 0xa5, 0x5e, 0x2a, 0x38, 0x71, 0x66, 0xab, 0x58, 0x87, 0xee, 0x0f, 0x0b,
	0x7a, 0x93, 0x88, 0x52, 0x36, 0x80, 0x7e, 0x26, 0x51, 0x24, 0xe1, 0x0a, 0x9d, 0xce, 0x49, 0xe7,
	0xd4, 0x0e, 0x4a, 0xac, 0xb8, 0x34, 0x94, 0xf2, 0x23, 0x17, 0x73, 0xc7, 0x2a, 0x38, 0x83, 0x15,
	0x27, 0x73, 0x49, 0xb8, 0x7a, 0x33, 0x77, 0xba, 0x05, 0x67, 0x30, 0x73, 0x60, 0x6f, 0xc1, 0x25,
	0xbd, 0xc5, 0xdc, 0xe9, 0x69, 0xca, 0x40, 0xd5, 0x15, 0xc5, 0x4b, 0x7c, 0xaf, 0xa6, 0xed, 0x14,
	0x5d, 0x06, 0x1b, 0x6e, 0x12, 0x7f, 0x43, 0x67, 0xf7, 0xa4, 0x73, 0xda, 0x0d, 0x4a, 0xac, 0x38,
	0x25, 0xe1, 0x73, 0x41, 0xce, 0x5e, 0xd1, 0x67, 0x30, 0x1b, 0x82, 0x3d, 0x5b, 0xc6, 0x98, 0xe8,
	0x79, 0x7d, 0x4d, 0x56, 0x09, 0x76, 0x1f, 0x60, 0x9a, 0x45, 0x11, 0x0a, 0xad, 0x6b, 0x6b, 0xdd,
	0x5a, 0x86, 0x31, 0xe8, 0x51, 0x9e, 0xa2, 0x03, 0xba, 0x51, 0xc7, 0xcc, 0x85, 0x83, 0x39, 0x4a,
	0x7a, 0x6d, 0x9c, 0xee, 0x6b, 0xae, 0x91, 0x73, 0x9f, 0xc3, 0xe1, 0x44, 0xac, 0xfd, 0x8c, 0x02,
	0xfc, 0x9a, 0xa1, 0x24, 0xf6, 0x14, 0xfa, 0x52, 0xcc, 0x50, 0x2d, 0x55, 0x2f, 0x72, 0xff, 0xec,
	0x96, 0x57, 0xee, 0xdb, 0x53, 0xe9, 0xa0, 0x2c, 0x70, 0x17, 0x70, 0x64, 0xba, 0x65, 0xca, 0x13,
	0x89, 0x8d, 0xcd, 0x74, 0x5a, 0x9b, 0x79, 0x08, 0x87, 0xd3, 0x9c, 0x50, 0x06, 0x48, 0x99, 0x48,
	0xd0, 0x1c, 0xa3, 0x99, 0x64, 0x77, 0x61, 0x07, 0x85, 0xe0, 0x62, 0x73, 0x8e, 0x02, 0x6c, 0x7c,
	0x5e, 0xe0, 0x4d, 0x7c, 0xea, 0xee, 0xff, 0xec, 0xf3, 0x05, 0xdc, 0x9e, 0x88, 0xf5, 0xbb, 0x2f,
	0xf3, 0x58, 0xc8, 0x6b, 0x59, 0x7d, 0x05, 0x77, 0x6a, 0x02, 0xff, 0xe0, 0xb6, 0xf4, 0x61, 0x5d,
	0xf5, 0x11, 0xe0, 0x8a, 0xaf, 0xf1, 0x06, 0x3e, 0x8c, 0xc0, 0xb5, 0x7d, 0x8c, 0xe1, 0xf8, 0x3c,
	0xa3, 0x05, 0x26, 0x14, 0xcf, 0x42, 0xc2, 0x0f, 0x5c, 0x29, 0x52, 0x69, 0xe9, 0x01, 0xf4, 0xc2,
	0x8c, 0x16, 0xbf, 0xb3, 0xa3, 0x49, 0x77, 0x04, 0xc3, 0xed, 0x1a, 0x95, 0x2b, 0xb1, 0x89, 0x8d,
	0x2b, 0x83, 0xcf, 0x7e, 0x5a, 0x60, 0x6b, 0x29, 0x5c, 0x86, 0x39, 0x1b, 0x41, 0xf7, 0x02, 0x89,
	0x39, 0xf5, 0x39, 0xf5, 0xaf, 0x6a, 0x70, 0x6f, 0x0b, 0xb3, 0x99, 0x32, 0x82, 0xae, 0x9f, 0x5d,
	0xe9, 0xad, 0xfe, 0x9c, 0x76, 0x6f, 0xfd, 0xaf, 0x78, 0x09, 0xbb, 0xc5, 0x45, 0xd9, 0x71, 0xb3,
	0xa8, 0xf1, 0xa1, 0x0c, 0x86, 0xdb, 0xc9, 0x4a, 0xa4, 0x38, 0x47, 0x5b, 0xa4, 0x71, 0xe5, 0xb6,
	0x48, 0xeb, 0x82, 0x9f, 0xe1, 0xa0, 0xbe, 0x4b, 0xf6, 0xb8, 0x56, 0xfd, 0x87, 0x43, 0x0d, 0x9e,
	0xfc, 0xb5, 0xae, 0x18, 0x30, 0xf6, 0xe1, 0x11, 0x17, 0x97, 0x5e, 0x78, 0x19, 0xae, 0x31, 0x5d,
	0x86, 0x14, 0x71, 0xb1, 0xf2, 0x48, 0x84, 0x89, 0x8c, 0x50, 0x14, 0x0f, 0xb8, 0x96, 0x1a, 0x1f,
	0x9d, 0xab, 0x12, 0x75, 0x1b, 0x5f, 0x25, 0xfd, 0xce, 0xa7, 0xea, 0x5d, 0xff, 0x6e, 0xd9, 0x25,
	0x3b, 0xdd, 0xd5, 0xa9, 0x67, 0xbf, 0x02, 0x00, 0x00, 0xff, 0xff, 0xcc, 0xd8, 0xca, 0xe0, 0x03,
	0x06, 0x00, 0x00,
}

// Reference imports to suppress errors if they are not otherwise used.
var _ context.Context
var _ grpc.ClientConn

// This is a compile-time assertion to ensure that this generated file
// is compatible with the grpc package it is being compiled against.
const _ = grpc.SupportPackageIsVersion4

// SftpRelayClient is the client API for SftpRelay service.
//
// For semantics around ctx use and closing/ending streaming RPCs, please refer to https://godoc.org/google.golang.org/grpc#ClientConn.NewStream.
type SftpRelayClient interface {
	Get(ctx context.Context, in *SrvGetRequest, opts ...grpc.CallOption) (*SrvGetResponse, error)
	Put(ctx context.Context, in *SrvPutRequest, opts ...grpc.CallOption) (*SrvPutResponse, error)
	Mkdirs(ctx context.Context, in *SrvMkdirsRequest, opts ...grpc.CallOption) (*SrvMkdirsResponse, error)
	Remove(ctx context.Context, in *SrvRemoveRequest, opts ...grpc.CallOption) (*SrvRemoveResponse, error)
	Authenticate(ctx context.Context, in *AuthenticateToRemoteRequest, opts ...grpc.CallOption) (*AuthenticateToRemoteResponse, error)
}

type sftpRelayClient struct {
	cc *grpc.ClientConn
}

func NewSftpRelayClient(cc *grpc.ClientConn) SftpRelayClient {
	return &sftpRelayClient{cc}
}

func (c *sftpRelayClient) Get(ctx context.Context, in *SrvGetRequest, opts ...grpc.CallOption) (*SrvGetResponse, error) {
	out := new(SrvGetResponse)
	err := c.cc.Invoke(ctx, "/sftpproto.SftpRelay/Get", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *sftpRelayClient) Put(ctx context.Context, in *SrvPutRequest, opts ...grpc.CallOption) (*SrvPutResponse, error) {
	out := new(SrvPutResponse)
	err := c.cc.Invoke(ctx, "/sftpproto.SftpRelay/Put", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *sftpRelayClient) Mkdirs(ctx context.Context, in *SrvMkdirsRequest, opts ...grpc.CallOption) (*SrvMkdirsResponse, error) {
	out := new(SrvMkdirsResponse)
	err := c.cc.Invoke(ctx, "/sftpproto.SftpRelay/Mkdirs", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *sftpRelayClient) Remove(ctx context.Context, in *SrvRemoveRequest, opts ...grpc.CallOption) (*SrvRemoveResponse, error) {
	out := new(SrvRemoveResponse)
	err := c.cc.Invoke(ctx, "/sftpproto.SftpRelay/Remove", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

func (c *sftpRelayClient) Authenticate(ctx context.Context, in *AuthenticateToRemoteRequest, opts ...grpc.CallOption) (*AuthenticateToRemoteResponse, error) {
	out := new(AuthenticateToRemoteResponse)
	err := c.cc.Invoke(ctx, "/sftpproto.SftpRelay/Authenticate", in, out, opts...)
	if err != nil {
		return nil, err
	}
	return out, nil
}

// SftpRelayServer is the server API for SftpRelay service.
type SftpRelayServer interface {
	Get(context.Context, *SrvGetRequest) (*SrvGetResponse, error)
	Put(context.Context, *SrvPutRequest) (*SrvPutResponse, error)
	Mkdirs(context.Context, *SrvMkdirsRequest) (*SrvMkdirsResponse, error)
	Remove(context.Context, *SrvRemoveRequest) (*SrvRemoveResponse, error)
	Authenticate(context.Context, *AuthenticateToRemoteRequest) (*AuthenticateToRemoteResponse, error)
}

// UnimplementedSftpRelayServer can be embedded to have forward compatible implementations.
type UnimplementedSftpRelayServer struct {
}

func (*UnimplementedSftpRelayServer) Get(ctx context.Context, req *SrvGetRequest) (*SrvGetResponse, error) {
	return nil, status.Errorf(codes.Unimplemented, "method Get not implemented")
}
func (*UnimplementedSftpRelayServer) Put(ctx context.Context, req *SrvPutRequest) (*SrvPutResponse, error) {
	return nil, status.Errorf(codes.Unimplemented, "method Put not implemented")
}
func (*UnimplementedSftpRelayServer) Mkdirs(ctx context.Context, req *SrvMkdirsRequest) (*SrvMkdirsResponse, error) {
	return nil, status.Errorf(codes.Unimplemented, "method Mkdirs not implemented")
}
func (*UnimplementedSftpRelayServer) Remove(ctx context.Context, req *SrvRemoveRequest) (*SrvRemoveResponse, error) {
	return nil, status.Errorf(codes.Unimplemented, "method Remove not implemented")
}
func (*UnimplementedSftpRelayServer) Authenticate(ctx context.Context, req *AuthenticateToRemoteRequest) (*AuthenticateToRemoteResponse, error) {
	return nil, status.Errorf(codes.Unimplemented, "method Authenticate not implemented")
}

func RegisterSftpRelayServer(s *grpc.Server, srv SftpRelayServer) {
	s.RegisterService(&_SftpRelay_serviceDesc, srv)
}

func _SftpRelay_Get_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(SrvGetRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(SftpRelayServer).Get(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/sftpproto.SftpRelay/Get",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(SftpRelayServer).Get(ctx, req.(*SrvGetRequest))
	}
	return interceptor(ctx, in, info, handler)
}

func _SftpRelay_Put_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(SrvPutRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(SftpRelayServer).Put(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/sftpproto.SftpRelay/Put",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(SftpRelayServer).Put(ctx, req.(*SrvPutRequest))
	}
	return interceptor(ctx, in, info, handler)
}

func _SftpRelay_Mkdirs_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(SrvMkdirsRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(SftpRelayServer).Mkdirs(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/sftpproto.SftpRelay/Mkdirs",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(SftpRelayServer).Mkdirs(ctx, req.(*SrvMkdirsRequest))
	}
	return interceptor(ctx, in, info, handler)
}

func _SftpRelay_Remove_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(SrvRemoveRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(SftpRelayServer).Remove(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/sftpproto.SftpRelay/Remove",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(SftpRelayServer).Remove(ctx, req.(*SrvRemoveRequest))
	}
	return interceptor(ctx, in, info, handler)
}

func _SftpRelay_Authenticate_Handler(srv interface{}, ctx context.Context, dec func(interface{}) error, interceptor grpc.UnaryServerInterceptor) (interface{}, error) {
	in := new(AuthenticateToRemoteRequest)
	if err := dec(in); err != nil {
		return nil, err
	}
	if interceptor == nil {
		return srv.(SftpRelayServer).Authenticate(ctx, in)
	}
	info := &grpc.UnaryServerInfo{
		Server:     srv,
		FullMethod: "/sftpproto.SftpRelay/Authenticate",
	}
	handler := func(ctx context.Context, req interface{}) (interface{}, error) {
		return srv.(SftpRelayServer).Authenticate(ctx, req.(*AuthenticateToRemoteRequest))
	}
	return interceptor(ctx, in, info, handler)
}

var _SftpRelay_serviceDesc = grpc.ServiceDesc{
	ServiceName: "sftpproto.SftpRelay",
	HandlerType: (*SftpRelayServer)(nil),
	Methods: []grpc.MethodDesc{
		{
			MethodName: "Get",
			Handler:    _SftpRelay_Get_Handler,
		},
		{
			MethodName: "Put",
			Handler:    _SftpRelay_Put_Handler,
		},
		{
			MethodName: "Mkdirs",
			Handler:    _SftpRelay_Mkdirs_Handler,
		},
		{
			MethodName: "Remove",
			Handler:    _SftpRelay_Remove_Handler,
		},
		{
			MethodName: "Authenticate",
			Handler:    _SftpRelay_Authenticate_Handler,
		},
	},
	Streams:  []grpc.StreamDesc{},
	Metadata: "sftp.proto",
}
