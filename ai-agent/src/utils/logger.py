"""
Logging configuration
"""
import logging
import sys
from datetime import datetime


def setup_logger(name: str = __name__, level: int = logging.INFO) -> logging.Logger:
    """로깅 설정"""
    logger = logging.getLogger(name)
    
    if logger.hasHandlers():
        return logger
    
    logger.setLevel(level)
    
    # Console handler
    handler = logging.StreamHandler(sys.stdout)
    handler.setLevel(level)
    
    # Formatter
    formatter = logging.Formatter(
        fmt='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S'
    )
    handler.setFormatter(formatter)
    
    logger.addHandler(handler)
    return logger


# Default logger
logger = setup_logger("multiagent_chatbot")