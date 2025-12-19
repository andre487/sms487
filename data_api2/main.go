package main

import (
	"context"
	"crypto/subtle"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"log"
	"log/slog"
	"net/http"
	"os"
	"strings"
	"time"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/credentials"
	"github.com/aws/aws-sdk-go-v2/service/sqs"
	"github.com/google/uuid"
)

type ctxKey string

const requestIDKey ctxKey = "request_id"

func WithRequestID(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		reqid := uuid.NewString()
		ctx := context.WithValue(r.Context(), requestIDKey, reqid)
		w.Header().Set("X-Request-Id", reqid)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

type StatusWriter struct {
	http.ResponseWriter
	status int
}

func (w *StatusWriter) WriteHeader(code int) {
	w.status = code
	w.ResponseWriter.WriteHeader(code)
}

func AccessLog(logger *slog.Logger, next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		sw := &StatusWriter{ResponseWriter: w, status: 200}
		start := time.Now()
		next.ServeHTTP(sw, r)
		d := time.Since(start)

		reqid, _ := r.Context().Value(requestIDKey).(string)
		logger.Info(
			"http_request",
			"request_id", reqid,
			"method", r.Method,
			"path", r.URL.Path,
			"status", sw.status,
			"duration_ms", d.Milliseconds(),
			"remote", r.RemoteAddr,
		)
	})
}

func BasicAuth(user, pass string, logger *slog.Logger, next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		const prefix = "Basic "

		h := r.Header.Get("Authorization")
		if !strings.HasPrefix(h, prefix) {
			logger.Debug("Invalid Authorization header prefix")
			Unauthorized(w)
			return
		}

		raw, err := base64.StdEncoding.DecodeString(strings.TrimPrefix(h, prefix))
		if err != nil {
			logger.Debug("Invalid Authorization header base64")
			Unauthorized(w)
			return
		}

		parts := strings.SplitN(string(raw), ":", 2)
		if len(parts) != 2 {
			logger.Debug("Invalid Authorization header parts")
			Unauthorized(w)
			return
		}

		u := parts[0]
		p := parts[1]

		// Константное сравнение, чтобы не светить таймингом.
		if subtle.ConstantTimeCompare([]byte(u), []byte(user)) != 1 ||
			subtle.ConstantTimeCompare([]byte(p), []byte(pass)) != 1 {
			logger.Warn("Invalid Authorization data")
			Unauthorized(w)
			return
		}

		next.ServeHTTP(w, r)
	})
}

func Unauthorized(w http.ResponseWriter) {
	w.Header().Set("WWW-Authenticate", `Basic realm="restricted", charset="UTF-8"`)
	http.Error(w, "Unauthorized", http.StatusUnauthorized)
}

type RequestItem struct {
	DeviceId    string `json:"device_id"`
	MessageType string `json:"message_type"`
	DateTime    string `json:"date_time"`
	SmsDateTime string `json:"sms_date_time"`
	Tel         string `json:"tel"`
	Text        string `json:"text"`
}

type SqsMessageData struct {
	MessageType          string `json:"message_type"`
	PrintableMessageType string `json:"printable_message_type"`
	DeviceId             string `json:"device_id"`
	Tel                  string `json:"tel"`
	DateTime             string `json:"date_time"`
	PrintableDateTime    string `json:"printable_date_time"`
	SmsDateTime          string `json:"sms_date_time"`
	Marked               bool   `json:"marked"`
	Text                 string `json:"text"`
}

type SqsMessage struct {
	Type string           `json:"type"`
	Data []SqsMessageData `json:"data"`
}

func CreateSqsClient(ctx context.Context) *sqs.Client {
	endpoint := os.Getenv("SQS_ENDPOINT")
	if endpoint == "" {
		endpoint = "https://message-queue.api.cloud.yandex.net/"
	}

	region := os.Getenv("SQS_REGION")
	if region == "" {
		region = "ru-central1"
	}

	accessKey := os.Getenv("SQS_ACCESS_KEY")
	if accessKey == "" {
		log.Fatal("SQS_ACCESS_KEY is empty")
	}

	secretKey := os.Getenv("SQS_SECRET_KEY")
	if secretKey == "" {
		log.Fatal("SQS_SECRET_KEY is empty")
	}

	cfg, err := config.LoadDefaultConfig(
		ctx,
		config.WithRegion(region),
		config.WithCredentialsProvider(credentials.NewStaticCredentialsProvider(accessKey, secretKey, "")),
	)
	if err != nil {
		log.Fatalf("LoadDefaultConfig: %v", err)
	}

	return sqs.NewFromConfig(cfg, func(o *sqs.Options) {
		o.BaseEndpoint = aws.String(endpoint)
	})
}

func GetLogLevel() slog.Level {
	strLogLevel := os.Getenv("LOG_LEVEL")

	switch strings.ToLower(strLogLevel) {
	case "debug":
		return slog.LevelDebug
	case "":
		return slog.LevelInfo
	case "info":
		return slog.LevelInfo
	case "warn":
		return slog.LevelWarn
	case "warning":
		return slog.LevelWarn
	case "error":
		return slog.LevelError
	}

	log.Fatalf("Unknown log level: %s", strLogLevel)
	return slog.LevelInfo
}

func main() {
	listenAddr := os.Getenv("LISTEN_ADDR")
	listenPort := os.Getenv("LISTEN_PORT")
	httpUser := os.Getenv("HTTP_USER")
	httpPassword := os.Getenv("HTTP_PASSWORD")

	sqsQueueURL := os.Getenv("SQS_QUEUE_URL")
	if sqsQueueURL == "" {
		log.Fatal("SQS_QUEUE_URL is empty")
	}

	timeFormat := os.Getenv("TIME_FORMAT")
	if timeFormat == "" {
		timeFormat = "02 Jan 2006 15:04:05 UTC"
	}

	logLevel := GetLogLevel()
	h := slog.NewTextHandler(os.Stderr, &slog.HandlerOptions{Level: logLevel})
	logger := slog.New(h)

	if httpUser == "" || httpPassword == "" {
		log.Fatal("$HTTP_USER and $HTTP_PASSWORD must be set")
	}

	sqsCtx := context.Background()
	sqsClient := CreateSqsClient(sqsCtx)

	mux := http.NewServeMux()

	mux.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/" {
			http.NotFound(w, r)
			return
		}
		w.WriteHeader(http.StatusOK)
		_, err := w.Write([]byte("OK"))
		if err != nil {
			logger.Error(fmt.Sprintf("Error writing HTTP body: %s", err))
		}
	})

	mux.HandleFunc("/robots.txt", func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/robots.txt" {
			http.NotFound(w, r)
			return
		}
		w.WriteHeader(http.StatusOK)
		w.Header().Set("Content-Type", "text/plain")
		_, err := w.Write([]byte("Disallow: /\n"))
		if err != nil {
			logger.Error(fmt.Sprintf("Error writing HTTP body: %s", err))
		}
	})

	addSms := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.URL.Path != "/add-sms" {
			http.NotFound(w, r)
			return
		}
		if r.Method != http.MethodPost {
			http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
			return
		}

		if ct := r.Header.Get("Content-Type"); !strings.HasPrefix(ct, "application/json") {
			http.Error(w, "Content-Type not supported", http.StatusUnsupportedMediaType)
			return
		}
		defer func() {
			err := r.Body.Close()
			if err != nil {
				logger.Warn(fmt.Sprintf("Error closing body: %s", err))
			}
		}()

		var items []RequestItem
		dec := json.NewDecoder(r.Body)
		if err := dec.Decode(&items); err != nil {
			http.Error(w, "Bad json: "+err.Error(), http.StatusBadRequest)
			return
		}
		if dec.More() {
			http.Error(w, "Extra data after JSON", http.StatusBadRequest)
			return
		}

		var sqsMsg SqsMessage
		sqsMsg.Type = "new_messages"
		for _, item := range items {
			if item.Tel == "org.telegram.messenger" {
				continue
			}
			sqsMsg.Data = append(sqsMsg.Data, CreateSqsMessageData(item, timeFormat, logger))
		}

		smsCount := len(sqsMsg.Data)
		if smsCount > 0 {
			b, err := json.Marshal(sqsMsg)
			if err != nil {
				logger.Error(fmt.Sprintf("Error marshaling sqs message: %s", err))
				http.Error(w, "Internal Server Error", http.StatusInternalServerError)
				return
			}
			s := string(b)

			out, err := sqsClient.SendMessage(sqsCtx, &sqs.SendMessageInput{
				QueueUrl:       aws.String(sqsQueueURL),
				MessageBody:    aws.String(s),
				MessageGroupId: aws.String("1"),
			})
			if err != nil {
				logger.Error(fmt.Sprintf("Error sending sqs message: %s", err))
				http.Error(w, "Internal Server Error", http.StatusInternalServerError)
				return
			}
			logger.Info(fmt.Sprintf("SQS message sent: %s", *out.MessageId))
		}

		w.Header().Set("Content-Type", "application/json")
		resp := fmt.Sprintf(`{"status":"OK","added":%d}`, smsCount)
		if _, err := w.Write([]byte(resp)); err != nil {
			logger.Error(fmt.Sprintf("Error writing HTTP body: %s", err))
		}
	})
	mux.Handle("/add-sms", BasicAuth(httpUser, httpPassword, logger, addSms))

	handler := WithRequestID(AccessLog(logger, mux))

	if listenAddr == "" {
		listenAddr = "localhost"
	}
	if listenPort == "" {
		listenPort = "8080"
	}
	addr := fmt.Sprintf("%s:%s", listenAddr, listenPort)
	logger.Info(fmt.Sprintf("Listening on http://%s", addr))

	err := http.ListenAndServe(addr, handler)
	if err != nil {
		logger.Error(fmt.Sprintf("Server error: %s", err))
		os.Exit(1)
	}
}

func CreateSqsMessageData(item RequestItem, timeFormat string, logger *slog.Logger) SqsMessageData {
	var data SqsMessageData
	data.MessageType = item.MessageType
	if data.MessageType == "" {
		data.MessageType = "sms"
		logger.Warn("Message type is empty, using 'sms'")
	}
	data.DeviceId = item.DeviceId
	data.Tel = item.Tel
	data.DateTime = item.DateTime
	data.SmsDateTime = item.SmsDateTime
	data.Marked = false
	data.Text = item.Text

	if data.MessageType == "sms" {
		data.PrintableMessageType = "SMS"
	} else if data.MessageType == "notification" {
		data.PrintableMessageType = "Notification"
	} else {
		data.PrintableMessageType = fmt.Sprintf("Type %s", item.MessageType)
	}

	dt, err := ParseDateUTC(item.DateTime)
	if err == nil {
		data.PrintableDateTime = dt.UTC().Format(timeFormat)
	} else {
		logger.Warn(fmt.Sprintf("Error parsing date: %s", err))
		data.PrintableDateTime = item.DateTime
	}

	return data
}

func ParseDateUTC(s string) (time.Time, error) {
	s = strings.TrimSpace(s)
	if s == "" {
		return time.Time{}, fmt.Errorf("empty date string")
	}

	layouts := []string{
		"2006-01-02 15:04 +0000",
		"2006-01-02T15:04:05Z",
		"2006-01-02T15:04:05",
		time.RFC3339Nano,
		time.RFC3339,
		"2006-01-02 15:04:05Z07:00",
		"2006-01-02 15:04:05Z",
		"2006-01-02 15:04:05",
		"2006-01-02 15:04",
		"2006-01-02",
		"02.01.2006 15:04:05",
		"02.01.2006 15:04",
		"02.01.2006",
		"02/01/2006 15:04:05",
		"02/01/2006",
		"Jan 2, 2006 15:04:05",
		"Jan 2, 2006 15:04",
		"Jan 2, 2006",
	}

	var lastErr error
	for _, layout := range layouts {
		t, err := time.Parse(layout, s)
		if err == nil {
			return t.UTC(), nil
		}
		lastErr = err
	}

	return time.Time{}, fmt.Errorf("cannot parse %q: %w", s, lastErr)
}
