"""Logging configuration with OpenTelemetry trace context."""

import logging
import os
from opentelemetry import trace
from opentelemetry.sdk.logs import LoggerProvider
from opentelemetry.sdk.logs.export import BatchLogRecordProcessor
from opentelemetry.exporter.otlp.proto.http._log_exporter import OTLPLogExporter

class TraceContextFormatter(logging.Formatter):
    """Custom formatter that includes trace and span IDs in log records."""
    
    def format(self, record):
        # Get current span context
        current_span = trace.get_current_span()
        if current_span:
            span_context = current_span.get_span_context()
            if span_context.is_valid:
                # Add trace and span IDs to the log record
                record.trace_id = f"{span_context.trace_id:032x}"
                record.span_id = f"{span_context.span_id:016x}"
            else:
                record.trace_id = "00000000000000000000000000000000"
                record.span_id = "0000000000000000"
        else:
            record.trace_id = "00000000000000000000000000000000"
            record.span_id = "0000000000000000"
            
        return super().format(record)

def configure_logging():
    """Configure logging with trace context."""
    
    # Create custom formatter with trace context
    formatter = TraceContextFormatter(
        fmt='%(asctime)s [%(levelname)s] [%(trace_id)s,%(span_id)s] %(name)s - %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S'
    )
    
    # Configure console handler
    console_handler = logging.StreamHandler()
    console_handler.setFormatter(formatter)
    
    # Configure root logger
    root_logger = logging.getLogger()
    root_logger.setLevel(logging.INFO)
    root_logger.addHandler(console_handler)
    
    # Set specific log levels
    logging.getLogger('kafka').setLevel(logging.WARNING)
    logging.getLogger('opentelemetry').setLevel(logging.WARNING)
    
    # Configure OTEL log export if endpoint is available
    otel_endpoint = os.getenv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4318")
    if otel_endpoint:
        try:
            logger_provider = LoggerProvider()
            otlp_exporter = OTLPLogExporter(
                endpoint=f"{otel_endpoint}/v1/logs"
            )
            logger_provider.add_log_record_processor(
                BatchLogRecordProcessor(otlp_exporter)
            )
            logging.getLogger(__name__).info("OTEL log exporter configured")
        except Exception as e:
            logging.getLogger(__name__).warning(f"Failed to configure OTEL log exporter: {e}")

if __name__ == "__main__":
    configure_logging()
    logger = logging.getLogger("test")
    logger.info("Test log message with trace context")