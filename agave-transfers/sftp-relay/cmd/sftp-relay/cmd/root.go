package cmd

import (
	"entrogo.com/sshpool/pkg/clientpool"
	"fmt"
	agaveproto "github.com/agaveplatform/science-apis/agave-transfers/sftp-relay/pkg/sftpproto"
	"github.com/agaveplatform/science-apis/agave-transfers/sftp-relay/pkg/sftprelay"
	grpc_prometheus "github.com/grpc-ecosystem/go-grpc-prometheus"
	"github.com/mitchellh/go-homedir"
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"github.com/sirupsen/logrus"
	"github.com/spf13/cobra"
	"github.com/spf13/viper"
	"google.golang.org/grpc"
	"google.golang.org/grpc/health"
	hpb "google.golang.org/grpc/health/grpc_health_v1"
	"google.golang.org/grpc/keepalive"
	"log"
	"net"
	"net/http"
	"os"
	"time"
)

var (
	cfgFile     string
	listen      string
	verbose     bool
	debug     bool
	metricsPort int
	poolSize    int
	idleTimeout int

	metricsRegistry = prometheus.NewRegistry()           // prometheus metrics registry
	grpcMetrics     = grpc_prometheus.NewServerMetrics() // grpc metrics server

)

var rootCmd = &cobra.Command{
	Use:   "sftp-relay",
	Short: "A grpc sftp proxy server",
	Long: `This is a grpc microservice providing basic managed SFTP operations. It provides connection 
pooling and a convenient protobuf wrapper for interacting with one or more remote systems.`,
	Run: func(cmd *cobra.Command, args []string) {

		//// log to console and file
		//f, err := os.OpenFile("sftprelay.log", os.O_RDWR|os.O_CREATE|os.O_APPEND, 0666)
		//if err != nil {
		//	log.Fatalf("Error opening file: %v", err)
		//}
		//wrt := io.MultiWriter(os.Stdout, f)
		//log.SetOutput(wrt)

		// set up the protobuf server
		listen = viper.GetString("listen")
		lis, err := net.Listen("tcp", listen)
		if err != nil {
			log.Fatalf("Failed to listen: %v", err)
		}
		defer lis.Close()

		log.Println("Initializing grpc server")
		grpcServer := grpc.NewServer(
			grpc.StreamInterceptor(grpcMetrics.StreamServerInterceptor()),
			grpc.UnaryInterceptor(grpcMetrics.UnaryServerInterceptor()),
			grpc.KeepaliveParams(keepalive.ServerParameters{
				MaxConnectionIdle: 20 * time.Minute,
				Time:              (time.Duration(10) * time.Second),
				Timeout:           (time.Duration(10) * time.Second),
			}))

		poolSize = viper.GetInt("poolSize")
		idleTimeout = viper.GetInt("idleTimeout")
		log.Printf("Initializing sftp client pool with size %d", poolSize)
		cp := clientpool.New(
				clientpool.WithPoolSize(poolSize),
				clientpool.WithExpireAfter(time.Duration(idleTimeout) * time.Second))
		defer cp.Close()

		// Init a new api server to register with the grpc server
		server := sftprelay.Server{
			Registry:    *metricsRegistry,
			GrpcMetrics: *grpcMetrics,
			Pool:        cp,
		}
		// set up prometheus metrics
		server.InitMetrics()

		// init logging
		if (viper.GetBool("verbose")) {
			log.Println("Verbose logging level configured")
			server.SetLogLevel(logrus.TraceLevel)
		} else if (viper.GetBool("debug")) {
			log.Println("Debug logging level configured")
			server.SetLogLevel(logrus.DebugLevel)
		} else {
			log.Println("Falling back to Info logging level")
		}

		// register the grpc services
		agaveproto.RegisterSftpRelayServer(grpcServer, &server)
		hpb.RegisterHealthServer(grpcServer, health.NewServer())

		// Create a HTTP server for prometheus metrics scraping
		metricsPort = viper.GetInt("metricsPort")
		httpServer := &http.Server{
			Addr:              fmt.Sprintf("0.0.0.0:%d", metricsPort),
			Handler:           promhttp.HandlerFor(metricsRegistry, promhttp.HandlerOpts{}),
		}

		// start the http server in a goroutine
		go func() {
			log.Printf("sftp-relay metrics server listening -> 0.0.0.0:%d", metricsPort)
			if err := httpServer.ListenAndServe(); err != nil {
				log.Fatalf("Unable to start metrics server on port %d: %v", metricsPort, err.Error())
			}
		}()

		// start the grpc server
		log.Printf("sftp-relay grpc server listening -> %s", listen)
		if err = grpcServer.Serve(lis); err != nil {
			log.Fatalf("Unable to start GRPC server listening on port %d: %v", metricsPort, err.Error())
		}
	},
}

func Main() {
	if err := rootCmd.Execute(); err != nil {
		fmt.Println(err)
		os.Exit(1)
	}
}

// Execute adds all child commands to the root command and sets flags.
func Execute() {
	if err := rootCmd.Execute(); err != nil {
		log.Fatalf("Error executing root command: %v", err)
	}
}

func init() {
	cobra.OnInitialize(initConfig)

	//// log to console and file
	//f, err := os.OpenFile("sftp-relay.log", os.O_RDWR|os.O_CREATE|os.O_APPEND, 0666)
	//if err != nil {
	//	log.Fatalf("Error opening file: %v", err)
	//}
	//wrt := io.MultiWriter(os.Stdout, f)
	//log.SetOutput(wrt)

	// Here you will define your flags and configuration settings.
	// Cobra supports persistent flags, which, if defined here,
	// will be global for your application.
	pflags := rootCmd.PersistentFlags()

	pflags.StringVarP(&cfgFile, "config", "c", "", "Location of configuration file, if wanted instead of flags. (default is $HOME/.sftp-client.yaml)")
	pflags.BoolVarP(&verbose, "debug", "d", true, "Debug logging.")
	pflags.StringVarP(&listen, "listen", "l", ":50051", "Address on which to listen for gRPC requests.")
	pflags.BoolVarP(&verbose, "verbose", "v", false, "Verbose logging.")
	pflags.IntVar(&metricsPort, "metricsPort", 9092, "Port for Prometheus metrics service")
	pflags.IntVarP(&poolSize, "poolSize", "p", 10, "Maximum pool size")
	pflags.IntVarP(&idleTimeout, "idleTimeout", "i", 300, "Amount of time, in seconds, that an idle connection will be kept around before reaping.")

	viper.BindPFlag("debug", pflags.Lookup("debug"))
	viper.BindPFlag("listen", pflags.Lookup("listen"))
	viper.BindPFlag("verbose", pflags.Lookup("verbose"))
	viper.BindPFlag("metricsPort", pflags.Lookup("metricsPort"))
	viper.BindPFlag("poolSize", pflags.Lookup("poolSize"))
	viper.BindPFlag("idleTimeout", pflags.Lookup("idleTimeout"))
}

// initConfig reads in config file and ENV variables if set.
func initConfig() {
	if cfgFile != "" {
		// Use config file from the flag.
		viper.SetConfigFile(cfgFile)
	} else {
		// Find home directory.
		home, err := homedir.Dir()
		if err != nil {
			fmt.Println(err)
			os.Exit(1)
		}

		// Search config in home directory with name ".sftp-relay" (without extension).
		viper.AddConfigPath(home)
		viper.SetConfigName(".sftp-relay")
	}

	viper.SetEnvPrefix("AGAVE")
	viper.AutomaticEnv()

	// If a config file is found, read it in.
	if err := viper.ReadInConfig(); err == nil {
		fmt.Println("Using config file:", viper.ConfigFileUsed())
	}
}
