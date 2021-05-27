/*
Copyright © 2019 NAME HERE <EMAIL ADDRESS>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package cmd

import (
	"context"
	"fmt"
	"github.com/agaveplatform/science-apis/agave-transfers/sftp-relay/cmd/sftp-client/cmd/helper"
	agaveproto "github.com/agaveplatform/science-apis/agave-transfers/sftp-relay/pkg/sftpproto"
	"github.com/spf13/cobra"
	"github.com/spf13/viper"
	"os"
)

var newName string
var renameCmd = &cobra.Command{
	Use:   "rename",
	Short: "Makes a remote rename on a file or folder",
	Long: `Renames the remote file or directory. The newPath must have the same parent directory as the original.`,
	Run: func(cmd *cobra.Command, args []string) {
		//log.Infof("Rename Command =====================================")
		//log.Infof("remotePath = %v", remotePath)
		//log.Infof("newName = %v", newName)

		conn, err := helper.NewGrpcServiceConn()
		if err != nil {
			fmt.Printf("Unable to establish a connection to service at %s: %v", grpcservice, err.Error())
			os.Exit(1)
		}
		defer conn.Close()

		sftpRelay := agaveproto.NewSftpRelayClient(conn)

		//log.Println("Starting Remove rpc client: ")
		//startPushtime := time.Now()
		//log.Printf("Start Time = %v", startPushtime)

		req := &agaveproto.SrvStatRequest{
			SystemConfig: helper.ParseSftpConfig(cmd.Flags()),
			RemotePath: remotePath,
		}
		//log.Debugf("Connecting to grpc service at: %s:%d", host, port)

		res, err := sftpRelay.Stat(context.Background(), req)
		//secs := time.Since(startPushtime).Seconds()
		if err != nil {
			fmt.Printf("Error while calling gRPC Remove: %s\n", err.Error())
			os.Exit(1)
		} else if res == nil {
			fmt.Println("Empty response received from gRPC server")
		} else {
			if res.Error != "" {
				fmt.Printf("%s\n", res.Error)
			} else {
				fmt.Printf("%s\n", res.RemoteFileInfo.Path)
			}
		}
		//log.Debugf("%v", res)
		//log.Infof("End Time %f", time.Since(startPushtime).Seconds())
		//log.Info("RPC Get Time: " + strconv.FormatFloat(secs, 'f', -1, 64))
	},
}

func init() {
	rootCmd.AddCommand(renameCmd)

	renameCmd.Flags().StringVarP(&newName, "newName", "n", "", "New name for the remotePath.")

	viper.BindPFlag("newName", renameCmd.Flags().Lookup("newName"))

}
