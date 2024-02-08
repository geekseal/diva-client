import ShareItems from './ShareItems';
import { SharedSong } from '@/types/song';
import { useEffect } from 'react';
import { useFetch } from '@/hooks/useFetch';
import { req } from '@/services';

const ShareContent = () => {
  const [isLoading, sharedSongs, error, getSharedSongs] = useFetch<
    SharedSong[]
  >(req.post.getMyPosts);

  useEffect(() => {
    getSharedSongs();
  }, []);

  return (
    <>
      <div className="flex flex-col justify-center items-center gap-8 p-5">
        {sharedSongs?.map((song: SharedSong) => (
          <ShareItems key={song.postId} song={song}></ShareItems>
        ))}
      </div>
    </>
  );
};

export default ShareContent;