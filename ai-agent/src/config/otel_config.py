"""OpenTelemetry Configuration for AI Agent."""

import logging
import os
from typing import Optional

from opentelemetry import trace, metrics, logs
from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter
from opentelemetry.exporter.otlp.proto.http.metric_exporter import OTLPMetricExporter 
from opentelemetry.exporter.otlp.proto.http._log_exporter import OTLPLogExporter
from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
from opentelemetry.instrumentation.kafka import KafkaInstrumentor
from opentelemetry.instrumentation.logging import LoggingInstrumentor
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor
from opentelemetry.sdk.metrics import MeterProvider
from opentelemetry.sdk.metrics.export import PeriodicExportingMetricReader
from opentelemetry.sdk.logs import LoggerProvider
from opentelemetry.sdk.logs.export import BatchLogRecordProcessor
from opentelemetry.sdk.resources import Resource
from opentelemetry.semconv.resource import ResourceAttributes

logger = logging.getLogger(__name__)

class OTelConfig:
    """OpenTelemetry configuration manager."""
    
    def __init__(self):
        self.otel_endpoint = os.getenv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4318")
        self.service_name = os.getenv("OTEL_SERVICE_NAME", "ai-agent-multiagent")
        self.service_version = os.getenv("OTEL_SERVICE_VERSION", "1.0.0")
        self.environment = os.getenv("ENVIRONMENT", "development")
        
    def create_resource(self) -> Resource:
        """Create OpenTelemetry resource with service attributes."""
        return Resource.create({
            ResourceAttributes.SERVICE_NAME: self.service_name,
            ResourceAttributes.SERVICE_VERSION: self.service_version,
            ResourceAttributes.DEPLOYMENT_ENVIRONMENT: self.environment,
        })
        
    def setup_tracing(self) -> None:
        """Setup distributed tracing."""
        resource = self.create_resource()
        
        # Create tracer provider
        trace.set_tracer_provider(TracerProvider(resource=resource))
        
        # Setup OTLP span exporter
        otlp_exporter = OTLPSpanExporter(
            endpoint=f"{self.otel_endpoint}/v1/traces",
            headers={},
        )
        
        # Add span processor
        trace.get_tracer_provider().add_span_processor(
            BatchSpanProcessor(otlp_exporter)
        )
        
        logger.info(f"OpenTelemetry tracing configured for {self.service_name}")
        
    def setup_metrics(self) -> None:
        """Setup metrics collection."""
        resource = self.create_resource()
        
        # Create metric reader
        metric_reader = PeriodicExportingMetricReader(
            OTLPMetricExporter(
                endpoint=f"{self.otel_endpoint}/v1/metrics",
                headers={},
            ),
            export_interval_millis=30000,  # 30 seconds
        )
        
        # Set meter provider
        metrics.set_meter_provider(
            MeterProvider(resource=resource, metric_readers=[metric_reader])
        )
        
        logger.info(f"OpenTelemetry metrics configured for {self.service_name}")
        
    def setup_logging(self) -> None:
        """Setup log collection with trace correlation."""
        resource = self.create_resource()
        
        # Create logger provider
        logger_provider = LoggerProvider(resource=resource)
        logs.set_logger_provider(logger_provider)
        
        # Setup OTLP log exporter
        otlp_log_exporter = OTLPLogExporter(
            endpoint=f"{self.otel_endpoint}/v1/logs",
            headers={},
        )
        
        # Add log processor
        logger_provider.add_log_record_processor(
            BatchLogRecordProcessor(otlp_log_exporter)
        )
        
        # Instrument logging to include trace context
        LoggingInstrumentor().instrument(set_logging_format=True)
        
        logger.info(f"OpenTelemetry logging configured for {self.service_name}")
        
    def setup_instrumentations(self, app=None) -> None:
        """Setup automatic instrumentations."""
        # FastAPI instrumentation
        if app is not None:
            FastAPIInstrumentor.instrument_app(app)
            logger.info("FastAPI instrumentation enabled")
            
        # Kafka instrumentation 
        KafkaInstrumentor().instrument()
        logger.info("Kafka instrumentation enabled")
        
    def configure_all(self, app=None) -> None:
        """Configure all OpenTelemetry components."""
        try:
            self.setup_tracing()
            self.setup_metrics() 
            self.setup_logging()
            self.setup_instrumentations(app)
            
            logger.info("OpenTelemetry configuration completed successfully")
            
        except Exception as e:
            logger.error(f"Failed to configure OpenTelemetry: {e}")
            raise

def get_tracer(name: str = __name__):
    """Get OpenTelemetry tracer."""
    return trace.get_tracer(name)

def get_meter(name: str = __name__):
    """Get OpenTelemetry meter."""
    return metrics.get_meter(name)

# Global OTEL configuration instance
otel_config = OTelConfig()