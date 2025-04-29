import time
import wave
import os
import sys
import io
from config.logger import setup_logging
from typing import Optional, Tuple, List
import uuid
import opuslib_next
from core.providers.asr.base import ASRProviderBase

from funasr import AutoModel
from funasr.utils.postprocess_utils import rich_transcription_postprocess
import requests

TAG = __name__
logger = setup_logging()

class ASRProvider(ASRProviderBase):
    def __init__(self, config: dict, delete_audio_file: bool):
        self.api_key = config.get("api_key")
        self.api_url = config.get("base_url")
        self.model = config.get("model_name")        
        self.output_dir = config.get("output_dir")
        self.delete_audio_file = delete_audio_file

        os.makedirs(self.output_dir, exist_ok=True)

    async def speech_to_text(self, opus_data: List[bytes], session_id: str) -> Tuple[Optional[str], Optional[str]]:
        file_path = None
        try:
            file_path = self.save_audio_to_file(opus_data, session_id)
            logger.bind(tag=TAG).info(f"stt文件: {file_path}")
            headers = {
                "Authorization": f"Bearer {self.api_key}",
            }
            
            # 使用data参数传递模型名称
            data = {
                "model": self.model
            }
            
            with open(file_path, "rb") as audio_file:  # 使用with语句确保文件关闭
                files = {
                    "file": audio_file
                }
                
                response = requests.post(
                    self.api_url,
                    files=files,
                    data=data,
                    headers=headers
                )

            if response.status_code == 200:
                text = response.json().get("text", "")
                return text, file_path
            else:
                raise Exception(f"API请求失败: {response.status_code} - {response.text}")
                
        except Exception as e:
            logger.bind(tag=TAG).error(f"语音识别失败: {e}")
            return "", None
        finally:
            if self.delete_audio_file and file_path:
                try:
                    if os.path.exists(file_path):  # 添加存在性检查
                        os.remove(file_path)
                except Exception as e:
                    logger.bind(tag=TAG).error(f"文件删除失败: {file_path} | 错误: {str(e)}")  # 添加具体错误信息

    # 保留原有的save_audio_to_file方法
    def save_audio_to_file(self, opus_data: List[bytes], session_id: str) -> str:
        """将Opus音频数据解码并保存为WAV文件"""
        file_name = f"gpt_asr_{session_id}_{uuid.uuid4()}.wav"
        file_path = os.path.join(self.output_dir, file_name)

        decoder = opuslib_next.Decoder(16000, 1)  # 16kHz, 单声道
        pcm_data = []

        for opus_packet in opus_data:
            try:
                pcm_frame = decoder.decode(opus_packet, 960)  # 960 samples = 60ms
                pcm_data.append(pcm_frame)
            except opuslib_next.OpusError as e:
                logger.bind(tag=TAG).error(f"Opus解码错误: {e}", exc_info=True)

        with wave.open(file_path, "wb") as wf:
            wf.setnchannels(1)
            wf.setsampwidth(2)  # 2 bytes = 16-bit
            wf.setframerate(16000)
            wf.writeframes(b"".join(pcm_data))

        return file_path