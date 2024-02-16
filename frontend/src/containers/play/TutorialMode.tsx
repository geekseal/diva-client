import { useEffect, useRef, useState } from 'react';
import LyricsComponent from './LyricsComponent';
import { S3SongInfo } from '@/types/song';
import PlayMonitor from './PlayMonitor';
import { LyricsInterface } from '@/types/lyrics';
import { parseLyricsTiming } from '@/utils/parseLyricsTiming';
import { PitchInterface } from '@/types/pitch';
import { parsePitchDuration } from '@/utils/parsePitchDuration';
import { getMusicInfo } from '@/services/getMusicInfo';
import { arUrl, infoUrl, mrUrl } from '@/utils/getS3URL';
import { homePage } from '../home';
import ARGuide from './ARGuide';

const TutorialMode = ({
  onModeChange,
  song,
}: {
  onModeChange: Function;
  song: S3SongInfo;
}) => {
  const audioRef = useRef<HTMLAudioElement>(null);
  const arAudioRef = useRef<HTMLAudioElement>(null);
  const bpm = useRef<number>(0);
  const gap = useRef<number>(0);
  const musicUrl = useRef<string>(mrUrl(song));
  const [parsedLyrics, setLyrics] = useState<LyricsInterface[]>([]);
  const [parsedPitches, setPitches] = useState<PitchInterface[]>([]);
  const [currentSeconds, setCurrentSeconds] = useState<number>(0);

  const [isMrLoaded, setIsMrLoaded] = useState<boolean>(false);
  const [isArLoaded, setIsArLoaded] = useState<boolean>(false);

  const handleSeconds = () => {
    if (audioRef.current) setCurrentSeconds(audioRef.current.currentTime);
  };

  const handleAudioEnd = () => {
    setTimeout(() => onModeChange(homePage), 500);
  };

  useEffect(() => {
    getMusicInfo(infoUrl(song))
      .then((info) => info)
      .then((lyrics) => {
        const lyricsArray: string[] = lyrics.trim().split('\n');
        bpm.current = +lyricsArray[6].split(':')[1] * 4;
        gap.current = +lyricsArray[7].split(':')[1] / 1000;
        setLyrics(parseLyricsTiming(lyricsArray, bpm.current, gap.current));
        setPitches(parsePitchDuration(lyricsArray, bpm.current, gap.current));
      });

    // 모바일 환경에서 canplaythrough 이벤트 캐치를 위해
    audioRef.current?.load();
    arAudioRef.current?.load();

    return () => {
      audioRef.current?.removeEventListener('timeupdate', handleSeconds);
      audioRef.current?.removeEventListener('ended', handleAudioEnd);
    };
  }, []);

  useEffect(() => {
    if (isArLoaded && isMrLoaded) {
      audioRef.current?.play();
      if (audioRef.current) {
        arAudioRef.current!.volume = 0.01;
        arAudioRef.current?.play();
      }
      audioRef.current?.addEventListener('timeupdate', handleSeconds);
      audioRef.current?.addEventListener('ended', handleAudioEnd);
    }
  }, [isArLoaded, isMrLoaded]);

  return (
    <main className="flex flex-col">
      <PlayMonitor
        currentSeconds={currentSeconds}
        parsedPitches={parsedPitches}
      />
      <LyricsComponent
        currentSeconds={currentSeconds}
        parsedLyrics={parsedLyrics}
      />
      <ARGuide arAudioRef={arAudioRef} />

      <audio
        ref={arAudioRef}
        src={arUrl({ artist: song.artist, songTitle: song.songTitle })}
        onCanPlayThrough={() => {
          setIsArLoaded(true);
        }}
      ></audio>
      <audio
        ref={audioRef}
        onCanPlayThrough={() => {
          setIsMrLoaded(true);
        }}
        src={musicUrl.current}
      ></audio>
    </main>
  );
};

export default TutorialMode;
