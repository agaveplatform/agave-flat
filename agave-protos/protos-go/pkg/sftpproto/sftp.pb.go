// Code generated by protoc-gen-go. DO NOT EDIT.
// source: sftp.proto

package sftpproto

import proto "github.com/golang/protobuf/proto"
import fmt "fmt"
import math "math"

import (
	context "golang.org/x/net/context"
	grpc "google.golang.org/grpc"
)

// Reference imports to suppress errors if they are not otherwise used.
var _ = proto.Marshal
var _ = fmt.Errorf
var _ = math.Inf

// This is a compile-time assertion to ensure that this generated file
// is compatible with the proto package it is being compiled against.
// A compilation error at this line likely means your copy of the
// proto package needs to be updated.
const _ = proto.ProtoPackageIsVersion2 // please upgrade the proto package

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
	return fileDescriptor_sftp_fb837fd97de0b6d6, []int{0}
}
func (m *Sftp) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_Sftp.Unmarshal(m, b)
}
func (m *Sftp) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_Sftp.Marshal(b, m, deterministic)
}
func (dst *Sftp) XXX_Merge(src proto.Message) {
	xxx_messageInfo_Sftp.Merge(dst, src)
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
	return fileDescriptor_sftp_fb837fd97de0b6d6, []int{1}
}
func (m *SrvPutRequest) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_SrvPutRequest.Unmarshal(m, b)
}
func (m *SrvPutRequest) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_SrvPutRequest.Marshal(b, m, deterministic)
}
func (dst *SrvPutRequest) XXX_Merge(src proto.Message) {
	xxx_messageInfo_SrvPutRequest.Merge(dst, src)
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
	return fileDescriptor_sftp_fb837fd97de0b6d6, []int{2}
}
func (m *SrvPutResponse) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_SrvPutResponse.Unmarshal(m, b)
}
func (m *SrvPutResponse) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_SrvPutResponse.Marshal(b, m, deterministic)
}
func (dst *SrvPutResponse) XXX_Merge(src proto.Message) {
	xxx_messageInfo_SrvPutResponse.Merge(dst, src)
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
	return fileDescriptor_sftp_fb837fd97de0b6d6, []int{3}
}
func (m *SrvGetRequest) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_SrvGetRequest.Unmarshal(m, b)
}
func (m *SrvGetRequest) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_SrvGetRequest.Marshal(b, m, deterministic)
}
func (dst *SrvGetRequest) XXX_Merge(src proto.Message) {
	xxx_messageInfo_SrvGetRequest.Merge(dst, src)
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
	return fileDescriptor_sftp_fb837fd97de0b6d6, []int{4}
}
func (m *SrvGetResponse) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_SrvGetResponse.Unmarshal(m, b)
}
func (m *SrvGetResponse) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_SrvGetResponse.Marshal(b, m, deterministic)
}
func (dst *SrvGetResponse) XXX_Merge(src proto.Message) {
	xxx_messageInfo_SrvGetResponse.Merge(dst, src)
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
	return fileDescriptor_sftp_fb837fd97de0b6d6, []int{5}
}
func (m *AuthenticateToRemoteRequest) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_AuthenticateToRemoteRequest.Unmarshal(m, b)
}
func (m *AuthenticateToRemoteRequest) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_AuthenticateToRemoteRequest.Marshal(b, m, deterministic)
}
func (dst *AuthenticateToRemoteRequest) XXX_Merge(src proto.Message) {
	xxx_messageInfo_AuthenticateToRemoteRequest.Merge(dst, src)
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
	return fileDescriptor_sftp_fb837fd97de0b6d6, []int{6}
}
func (m *AuthenticateToRemoteResponse) XXX_Unmarshal(b []byte) error {
	return xxx_messageInfo_AuthenticateToRemoteResponse.Unmarshal(m, b)
}
func (m *AuthenticateToRemoteResponse) XXX_Marshal(b []byte, deterministic bool) ([]byte, error) {
	return xxx_messageInfo_AuthenticateToRemoteResponse.Marshal(b, m, deterministic)
}
func (dst *AuthenticateToRemoteResponse) XXX_Merge(src proto.Message) {
	xxx_messageInfo_AuthenticateToRemoteResponse.Merge(dst, src)
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
	proto.RegisterType((*AuthenticateToRemoteRequest)(nil), "sftpproto.AuthenticateToRemoteRequest")
	proto.RegisterType((*AuthenticateToRemoteResponse)(nil), "sftpproto.AuthenticateToRemoteResponse")
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
	Authenticate(context.Context, *AuthenticateToRemoteRequest) (*AuthenticateToRemoteResponse, error)
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
			MethodName: "Authenticate",
			Handler:    _SftpRelay_Authenticate_Handler,
		},
	},
	Streams:  []grpc.StreamDesc{},
	Metadata: "sftp.proto",
}

func init() { proto.RegisterFile("sftp.proto", fileDescriptor_sftp_fb837fd97de0b6d6) }

var fileDescriptor_sftp_fb837fd97de0b6d6 = []byte{
	// 468 bytes of a gzipped FileDescriptorProto
	0x1f, 0x8b, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0xff, 0xb4, 0x54, 0x4d, 0x6b, 0xdb, 0x40,
	0x10, 0x45, 0xb6, 0x93, 0x58, 0x93, 0x8f, 0xc2, 0xd2, 0xc3, 0xd6, 0x0d, 0x25, 0xa8, 0x5f, 0x81,
	0x82, 0x0e, 0xe9, 0xcd, 0xf4, 0x12, 0x1f, 0x1a, 0x4a, 0xa1, 0x08, 0xb9, 0x50, 0xe8, 0xa5, 0xac,
	0xed, 0x51, 0x6c, 0xb0, 0xb5, 0xea, 0xec, 0x28, 0xa0, 0xfe, 0x9c, 0xfe, 0x90, 0xfe, 0x9a, 0xfe,
	0x90, 0xb2, 0x2b, 0xaf, 0x24, 0xa7, 0x69, 0x7b, 0x28, 0xb9, 0xcd, 0x9b, 0x37, 0x6f, 0xe6, 0xe9,
	0x49, 0x08, 0xc0, 0x64, 0x5c, 0xc4, 0x05, 0x69, 0xd6, 0x22, 0xb4, 0xb5, 0x2b, 0xa3, 0x1f, 0x3d,
	0x18, 0x4c, 0x33, 0x2e, 0xc4, 0x08, 0x86, 0xa5, 0x41, 0xca, 0xd5, 0x06, 0x65, 0x70, 0x16, 0x9c,
	0x87, 0x69, 0x83, 0x2d, 0x57, 0x28, 0x63, 0x3e, 0x69, 0x5a, 0xc8, 0x5e, 0xcd, 0x79, 0x6c, 0x39,
	0x53, 0x19, 0xc6, 0xcd, 0xbb, 0x85, 0xec, 0xd7, 0x9c, 0xc7, 0x42, 0xc2, 0xc1, 0x52, 0x1b, 0x7e,
	0x8f, 0x95, 0x1c, 0x38, 0xca, 0x43, 0xab, 0xca, 0x56, 0x6b, 0xfc, 0x60, 0xaf, 0xed, 0xd5, 0x2a,
	0x8f, 0x3d, 0x37, 0x5d, 0x7d, 0x43, 0xb9, 0x7f, 0x16, 0x9c, 0xf7, 0xd3, 0x06, 0x5b, 0xce, 0xae,
	0x48, 0x34, 0xb1, 0x3c, 0xa8, 0x75, 0x1e, 0x8b, 0x53, 0x08, 0xe7, 0xeb, 0x15, 0xe6, 0xee, 0xde,
	0xd0, 0x91, 0x6d, 0x43, 0x3c, 0x01, 0x98, 0x95, 0x59, 0x86, 0xe4, 0xf6, 0x86, 0x6e, 0x6f, 0xa7,
	0x23, 0x04, 0x0c, 0xb8, 0x2a, 0x50, 0x82, 0x13, 0xba, 0x5a, 0x44, 0x70, 0xb4, 0x40, 0xc3, 0x6f,
	0xbd, 0xd3, 0x43, 0xc7, 0xed, 0xf4, 0xa2, 0x37, 0x70, 0x3c, 0xa5, 0x9b, 0xa4, 0xe4, 0x14, 0xbf,
	0x96, 0x68, 0x58, 0xbc, 0x82, 0xa1, 0xa1, 0x39, 0xda, 0x50, 0x5d, 0x90, 0x87, 0x17, 0x0f, 0xe2,
	0x26, 0xef, 0xd8, 0xb6, 0xd3, 0x66, 0x20, 0x5a, 0xc2, 0x89, 0x57, 0x9b, 0x42, 0xe7, 0x06, 0x77,
	0x92, 0x09, 0x6e, 0x25, 0xf3, 0x0c, 0x8e, 0x67, 0x15, 0xa3, 0x49, 0x91, 0x4b, 0xca, 0xd1, 0xbf,
	0x8c, 0xdd, 0xa6, 0x78, 0x08, 0x7b, 0x48, 0xa4, 0x69, 0xfb, 0x3a, 0x6a, 0xb0, 0xf5, 0x79, 0x85,
	0xff, 0xe3, 0xd3, 0xa9, 0xef, 0xd9, 0xe7, 0x04, 0x1e, 0x5f, 0x96, 0xbc, 0xc4, 0x9c, 0x57, 0x73,
	0xc5, 0xf8, 0x51, 0xa7, 0xb8, 0xd1, 0x8c, 0xde, 0xf5, 0x53, 0x18, 0xa8, 0x92, 0x97, 0x7f, 0x72,
	0xec, 0xc8, 0x68, 0x0c, 0xa7, 0x77, 0xef, 0x68, 0xbd, 0xd3, 0xb6, 0xf6, 0xde, 0x3d, 0xbe, 0xf8,
	0x19, 0x40, 0xe8, 0x56, 0xe1, 0x5a, 0x55, 0x62, 0x0c, 0xfd, 0x2b, 0x64, 0x21, 0xbb, 0x77, 0xba,
	0x29, 0x8e, 0x1e, 0xdd, 0xc1, 0x6c, 0xaf, 0x8c, 0xa1, 0x9f, 0x94, 0xbf, 0x69, 0xdb, 0x2f, 0xe5,
	0xb6, 0xb6, 0xfb, 0x15, 0x7c, 0x81, 0xa3, 0xee, 0x13, 0x88, 0x17, 0x9d, 0xd1, 0xbf, 0xc4, 0x33,
	0x7a, 0xf9, 0xcf, 0xb9, 0xfa, 0xc0, 0x24, 0x81, 0xe7, 0x9a, 0xae, 0x63, 0x75, 0xad, 0x6e, 0xb0,
	0x58, 0x2b, 0xce, 0x34, 0x6d, 0x62, 0x26, 0x95, 0x9b, 0x0c, 0xa9, 0xfe, 0x4d, 0xb8, 0x55, 0x93,
	0x93, 0x4b, 0x3b, 0x62, 0x13, 0x49, 0x6c, 0x33, 0x09, 0x3e, 0xb7, 0x7f, 0x8f, 0xef, 0xbd, 0xb0,
	0x61, 0x67, 0xfb, 0xae, 0xf5, 0xfa, 0x57, 0x00, 0x00, 0x00, 0xff, 0xff, 0x0e, 0x31, 0x77, 0x40,
	0x69, 0x04, 0x00, 0x00,
}
