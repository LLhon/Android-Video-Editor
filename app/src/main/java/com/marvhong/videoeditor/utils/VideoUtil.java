package com.marvhong.videoeditor.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AACTrackImpl;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;
import com.googlecode.mp4parser.authoring.tracks.TextTrackImpl;
import com.marvhong.videoeditor.model.LocalVideoModel;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * @author LLhon
 * @Project diaoyur_android
 * @Package com.kangoo.util.video
 * @Date 2018/4/11 12:25
 * @description 视频工具类
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class VideoUtil {

    private static final String TAG = VideoUtil.class.getSimpleName();
    public static final int VIDEO_MAX_DURATION = 10;// 15秒
    public static final int MIN_TIME_FRAME = 3;
    private static final int thumb_Width = (UIUtils.getScreenWidth() - UIUtils.dp2Px(16)) / VIDEO_MAX_DURATION;
    private static final int thumb_Height = UIUtils.dp2Px(62);
    private static final long one_frame_time = 1000000;
    public static final String POSTFIX = ".jpeg";
    private static final String TRIM_PATH = "small_video";
    private static final String THUMB_PATH = "thumb";

    /**
     * 获取视频的帧图片
     * @param context
     * @param videoUri
     */
    public static Observable<ArrayList<Bitmap>> backgroundShootVideoThumb(final Context context, final Uri videoUri) {

        return Observable.create(new ObservableOnSubscribe<ArrayList<Bitmap>>() {
            @Override
            public void subscribe(ObservableEmitter<ArrayList<Bitmap>> emitter) {
                ArrayList<Bitmap> thumbnailList = new ArrayList<>();
                try {
                    MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                    mediaMetadataRetriever.setDataSource(context, videoUri);
                    // Retrieve media data use microsecond
                    long videoLengthInMs = Long.parseLong(mediaMetadataRetriever
                        .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) * 1000;
                    long numThumbs =
                        videoLengthInMs < one_frame_time ? 1 : (videoLengthInMs / one_frame_time);
                    final long interval = videoLengthInMs / numThumbs;

                    //每次截取到3帧之后上报
                    for (long i = 0; i < numThumbs; ++i) {
                        Bitmap bitmap = mediaMetadataRetriever.getFrameAtTime(i * interval,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                        try {
                            bitmap = Bitmap
                                .createScaledBitmap(bitmap, thumb_Width, thumb_Height, false);
                        } catch (Exception e) {
                            e.printStackTrace();
                            emitter.onError(e);
                        }
                        thumbnailList.add(bitmap);
                        if (thumbnailList.size() == 3) {
                            emitter.onNext((ArrayList<Bitmap>) thumbnailList.clone());
                            thumbnailList.clear();
                        }
                    }
                    if (thumbnailList.size() > 0) {
                        emitter.onNext((ArrayList<Bitmap>) thumbnailList.clone());
                        thumbnailList.clear();
                    }
                    mediaMetadataRetriever.release();
                } catch (final Throwable e) {
                    e.printStackTrace();
                    emitter.onError(e);
                }
                emitter.onComplete();
            }
        })
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 获取本地相册中所有视频文件
     * @param context
     * @return
     */
    public static Observable<ArrayList<LocalVideoModel>> getLocalVideoFiles(final Context context) {

        return Observable.create(new ObservableOnSubscribe<ArrayList<LocalVideoModel>>() {
            @Override
            public void subscribe(ObservableEmitter<ArrayList<LocalVideoModel>> emitter) {
                ArrayList<LocalVideoModel> videoModels = new ArrayList<>();
                ContentResolver resolver = context.getContentResolver();
                try {
                    Cursor cursor = resolver
                        .query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null,
                            null, null, MediaStore.Video.Media.DATE_MODIFIED + " desc");
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            LocalVideoModel video = new LocalVideoModel();
                            if (cursor
                                .getLong(cursor.getColumnIndex(MediaStore.Video.Media.DURATION))
                                != 0) {
                                video.setDuration(
                                    cursor.getLong(
                                        cursor.getColumnIndex(MediaStore.Video.Media.DURATION)));
                                video.setVideoPath(
                                    cursor.getString(
                                        cursor.getColumnIndex(MediaStore.Video.Media.DATA)));
                                video.setCreateTime(cursor
                                    .getString(
                                        cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)));
                                video.setVideoName(cursor
                                    .getString(cursor
                                        .getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)));
                                videoModels.add(video);
                            }
                        }
                        emitter.onNext(videoModels);
                        cursor.close();
                    }
                } catch (Exception e) {
                    emitter.onError(e);
                }
                emitter.onComplete();
            }
        })
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 裁剪视频(异步操作)
     *
     * @param src 源文件
     * @param dest 输出地址
     * @param startSec 开始时间
     * @param endSec 结束时间
     */
    public static Observable<String> cutVideo(final String src, final String dest, final double startSec, final double endSec) {

        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) {
                try {
                    double startSecond = startSec;
                    double endSecond = endSec;
                    //构造一个movie对象
                    Movie movie = MovieCreator.build(src);
                    List<Track> tracks = movie.getTracks();
                    movie.setTracks(new ArrayList<Track>());

                    boolean timeCorrected = false;
                    // Here we try to find a track that has sync samples. Since we can only start decoding
                    // at such a sample we SHOULD make sure that the start of the new fragment is exactly
                    // such a frame
                    for (Track track : tracks) {
                        if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
                            if (timeCorrected) {
                                // This exception here could be a false positive in case we have multiple tracks
                                // with sync samples at exactly the same positions. E.g. a single movie containing
                                // multiple qualities of the same video (Microsoft Smooth Streaming file)

                                throw new RuntimeException(
                                    "The startTime has already been corrected by another track with SyncSample. Not Supported.");
                            }
                            //矫正开始时间
                            startSecond = correctTimeToSyncSample(track, startSecond, false);
                            //矫正结束时间
                            endSecond = correctTimeToSyncSample(track, endSecond, true);

                            timeCorrected = true;
                        }
                    }

                    //裁剪后的位置   startSecond:299400, endSecond:309390
                    //矫正后的位置   startSecond:291.3327083333511, endSecond:313.18787500003214
                    Log.e(TAG, "startSecond:" + startSecond + ", endSecond:" + endSecond);

                    //fix bug: 部分视频矫正过后会超出10s,这里进行强制限制在10s内
                    if (endSecond - startSecond > 10) {
                        int duration = (int) (endSec - startSec);
                        endSecond = startSecond + duration;
                    }
                    //fix bug: 部分视频裁剪后endSecond=0.0,导致播放失败
                    if (endSecond == 0.0) {
                        int duration = (int) (endSec - startSec);
                        endSecond = startSecond + duration;
                    }

                    for (Track track : tracks) {
                        long currentSample = 0;
                        double currentTime = 0;
                        double lastTime = -1;
                        long startSample = -1;
                        long endSample = -1;

                        for (int i = 0; i < track.getSampleDurations().length; i++) {
                            long delta = track.getSampleDurations()[i];

                            if (currentTime > lastTime && currentTime <= startSecond) {
                                // current sample is still before the new starttime
                                startSample = currentSample;
                            }
                            if (currentTime > lastTime && currentTime <= endSecond) {
                                // current sample is after the new start time and still before the new endtime
                                endSample = currentSample;
                            }

                            lastTime = currentTime;
                            //计算出某一帧的时长 = 采样时长 / 时间长度
                            currentTime +=
                                (double) delta / (double) track.getTrackMetaData().getTimescale();
                            //这里就是帧数（采样）加一
                            currentSample++;
                        }
                        //在这里，裁剪是根据关键帧进行裁剪的，而不是指定的开始时间和结束时间
                        //startSample:2453, endSample:2846   393
                        //startSample:4795, endSample:5564   769
                        Log.e(TAG, "startSample:" + startSample + ", endSample:" + endSample);
                        movie.addTrack(new CroppedTrack(track, startSample, endSample));

                        Container out = new DefaultMp4Builder().build(movie);
                        FileOutputStream fos = new FileOutputStream(String.format(dest));
                        FileChannel fc = fos.getChannel();
                        out.writeContainer(fc);

                        fc.close();
                        fos.close();
                    }

                    emitter.onNext(dest);

                } catch (Exception e) {
                    emitter.onError(e);
                }
                emitter.onComplete();
            }
        })
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 矫正裁剪的sample位置
     *
     * @param track 视频轨道
     * @param cutHere 裁剪位置
     * @param next 是否还继续裁剪
     */
    private static double correctTimeToSyncSample(Track track, double cutHere, boolean next) {
        double[] timeOfSyncSamples = new double[track.getSyncSamples().length];
        long currentSample = 0;
        double currentTime = 0;
        for (int i = 0; i < track.getSampleDurations().length; i++) {
            long delta = track.getSampleDurations()[i];

            if (Arrays.binarySearch(track.getSyncSamples(), currentSample + 1) >= 0) {
                // samples always start with 1 but we start with zero therefore +1（采样的下标从1开始而不是0开始，所以要+1 ）
                timeOfSyncSamples[Arrays
                    .binarySearch(track.getSyncSamples(), currentSample + 1)] = currentTime;
            }
            currentTime += (double) delta / (double) track.getTrackMetaData().getTimescale();
            currentSample++;

        }
        double previous = 0;
        for (double timeOfSyncSample : timeOfSyncSamples) {
            if (timeOfSyncSample > cutHere) {
                if (next) {
                    return timeOfSyncSample;
                } else {
                    return previous;
                }
            }
            previous = timeOfSyncSample;
        }
        return timeOfSyncSamples[timeOfSyncSamples.length - 1];
    }

    /**
     * Appends mp4 audio/video from {@code anotherFileName} to
     * {@code mainFileName}.
     */
    public static boolean append(String mainFileName, String anotherFileName) {
        boolean rvalue = false;
        try {
            File targetFile = new File(mainFileName);
            File anotherFile = new File(anotherFileName);
            if (targetFile.exists() && targetFile.length() > 0) {
                String tmpFileName = mainFileName + ".tmp";

                append(mainFileName, anotherFileName, tmpFileName);
                anotherFile.delete();
                targetFile.delete();
                new File(tmpFileName).renameTo(targetFile);
                rvalue = true;
            } else if (targetFile.createNewFile()) {
                copyFile(anotherFileName, mainFileName);
                anotherFile.delete();
                rvalue = true;
            }
        } catch (Exception tr) {
            Log.e("VideoUtils", "", tr);
        }
        return rvalue;
    }

    /**
     * 视频拼接
     *
     * @param srcFile 源文件
     * @param appendFile 待插入的文件
     * @param finalFile 最终生成的文件
     */
    public static void append(final String srcFile, final String appendFile, final String finalFile)
        throws IOException {

        final FileOutputStream fos = new FileOutputStream(new File(finalFile));
        final FileChannel fc = fos.getChannel();

        Movie movieSrc = null;
        try {
            movieSrc = MovieCreator.build(srcFile);
        } catch (Throwable tr) {
            tr.printStackTrace();
        }

        Movie movieAppend = null;
        try {
            movieAppend = MovieCreator.build(appendFile);
        } catch (Throwable tr) {
            tr.printStackTrace();
        }

        Movie finalMovie;
        if (movieSrc == null && movieAppend == null) {
            finalMovie = new Movie();
        } else if (movieSrc == null) {
            finalMovie = movieAppend;
        } else if (movieAppend == null) {
            finalMovie = movieSrc;
        } else {
            final List<Track> srcTracks = movieSrc.getTracks();
            final List<Track> appendTracks = movieAppend.getTracks();

            finalMovie = new Movie();
            for (int i = 0; i < srcTracks.size() || i < appendTracks.size(); ++i) {
                finalMovie.addTrack(new AppendTrack(srcTracks.get(i), appendTracks.get(i)));
            }
        }

        final Container container = new DefaultMp4Builder().build(finalMovie);
        container.writeContainer(fc);
        fc.close();
        fos.close();
    }

    /**
     * 复制文件
     */
    public static void copyFile(final String from, final String destination) throws IOException {
        FileInputStream in = new FileInputStream(from);
        FileOutputStream out = new FileOutputStream(destination);
        copy(in, out);
        in.close();
        out.close();
    }

    public static void copy(FileInputStream in, FileOutputStream out) throws IOException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
    }

    /**
     * 对Mp4文件集合进行追加合并(按照顺序一个一个拼接起来)
     *
     * @param mp4PathList [输入]Mp4文件路径的集合(支持m4a)(不支持wav)
     * @param outPutPath [输出]结果文件全部名称包含后缀(比如.mp4)
     * @throws IOException 格式不支持等情况抛出异常
     */
    public static void appendMp4List(List<String> mp4PathList, String outPutPath) {

        try {

            List<Movie> mp4MovieList = new ArrayList<>();// Movie对象集合[输入]
            for (String mp4Path : mp4PathList) {// 将每个文件路径都构建成一个Movie对象
                mp4MovieList.add(MovieCreator.build(mp4Path));
            }

            List<Track> audioTracks = new LinkedList<>();// 音频通道集合
            List<Track> videoTracks = new LinkedList<>();// 视频通道集合

            for (Movie mp4Movie : mp4MovieList) {// 对Movie对象集合进行循环
                for (Track inMovieTrack : mp4Movie.getTracks()) {
                    if ("soun".equals(inMovieTrack.getHandler())) {// 从Movie对象中取出音频通道
                        audioTracks.add(inMovieTrack);
                    }
                    if ("vide".equals(inMovieTrack.getHandler())) {// 从Movie对象中取出视频通道
                        videoTracks.add(inMovieTrack);
                    }
                }
            }
            Movie resultMovie = new Movie();// 结果Movie对象[输出]
            if (!audioTracks.isEmpty()) {// 将所有音频通道追加合并
                resultMovie
                    .addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
            }

            if (!videoTracks.isEmpty()) {// 将所有视频通道追加合并
                resultMovie
                    .addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
            }

            Container outContainer = new DefaultMp4Builder().build(resultMovie);// 将结果Movie对象封装进容器
            FileChannel fileChannel = new RandomAccessFile(String.format(outPutPath), "rw")
                .getChannel();
            outContainer.writeContainer(fileChannel);// 将容器内容写入磁盘
            fileChannel.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 对AAC文件集合进行追加合并(按照顺序一个一个拼接起来)
     *
     * @param aacPathList [输入]AAC文件路径的集合(不支持wav)
     * @param outPutPath [输出]结果文件全部名称包含后缀(比如.aac)
     * @throws IOException 格式不支持等情况抛出异常
     */
    public static void appendAacList(List<String> aacPathList, String outPutPath) {

        try {

            List<Track> audioTracks = new LinkedList<>();// 音频通道集合
            for (int i = 0; i < aacPathList.size(); i++) {// 将每个文件路径都构建成一个AACTrackImpl对象
                audioTracks.add(new AACTrackImpl(new FileDataSourceImpl(aacPathList.get(i))));
            }

            Movie resultMovie = new Movie();// 结果Movie对象[输出]
            if (!audioTracks.isEmpty()) {// 将所有音频通道追加合并
                resultMovie
                    .addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
            }

            Container outContainer = new DefaultMp4Builder().build(resultMovie);// 将结果Movie对象封装进容器
            FileChannel fileChannel = new RandomAccessFile(String.format(outPutPath), "rw")
                .getChannel();
            outContainer.writeContainer(fileChannel);// 将容器内容写入磁盘
            fileChannel.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private static List<Movie> moviesList = new ArrayList<>();
    private static List<Track> videoTracks = new ArrayList<>();
    private static List<Track> audioTracks = new ArrayList<>();

    //将两个mp4视频进行拼接
    public static void appendMp4(List<String> mMp4List, String outputpath) {

        try {
            for (int i = 0; i < mMp4List.size(); i++) {
                Movie movie = MovieCreator.build(mMp4List.get(i));
                moviesList.add(movie);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Movie m : moviesList) {
            for (Track t : m.getTracks()) {
                if (t.getHandler().equals("soun")) {
                    audioTracks.add(t);
                }
                if (t.getHandler().equals("vide")) {
                    videoTracks.add(t);
                }
            }
        }

        Movie result = new Movie();

        try {
            if (audioTracks.size() > 0) {
                result
                    .addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
            }
            if (videoTracks.size() > 0) {
                result
                    .addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Container out = new DefaultMp4Builder().build(result);

        try {
            FileChannel fc = new FileOutputStream(new File(outputpath)).getChannel();
            out.writeContainer(fc);
            fc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        moviesList.clear();
    }

    /**
     * 将 AAC 和 MP4 进行混合[替换了视频的音轨]
     *
     * @param aacPath .aac
     * @param mp4Path .mp4
     * @param outPath .mp4
     */
    public static boolean muxAacMp4(String aacPath, String mp4Path, String outPath) {
        boolean flag = false;
        try {
            AACTrackImpl aacTrack = new AACTrackImpl(new FileDataSourceImpl(aacPath));
            Movie videoMovie = MovieCreator.build(mp4Path);
            Track videoTracks = null;// 获取视频的单纯视频部分
            for (Track videoMovieTrack : videoMovie.getTracks()) {
                if ("vide".equals(videoMovieTrack.getHandler())) {
                    videoTracks = videoMovieTrack;
                }
            }

            Movie resultMovie = new Movie();
            resultMovie.addTrack(videoTracks);// 视频部分
            resultMovie.addTrack(aacTrack);// 音频部分

            Container out = new DefaultMp4Builder().build(resultMovie);
            FileOutputStream fos = new FileOutputStream(new File(outPath));
            out.writeContainer(fos.getChannel());
            fos.close();
            flag = true;
            Log.e("update_tag", "merge finish");
        } catch (Exception e) {
            e.printStackTrace();
            flag = false;
        }
        return flag;
    }


    /**
     * 将 M4A 和 MP4 进行混合[替换了视频的音轨]
     *
     * @param m4aPath .m4a[同样可以使用.mp4]
     * @param mp4Path .mp4
     * @param outPath .mp4
     */
    public static void muxM4AMp4(String m4aPath, String mp4Path, String outPath)
        throws IOException {
        Movie audioMovie = MovieCreator.build(m4aPath);
        Track audioTracks = null;// 获取视频的单纯音频部分
        for (Track audioMovieTrack : audioMovie.getTracks()) {
            if ("soun".equals(audioMovieTrack.getHandler())) {
                audioTracks = audioMovieTrack;
            }
        }

        Movie videoMovie = MovieCreator.build(mp4Path);
        Track videoTracks = null;// 获取视频的单纯视频部分
        for (Track videoMovieTrack : videoMovie.getTracks()) {
            if ("vide".equals(videoMovieTrack.getHandler())) {
                videoTracks = videoMovieTrack;
            }
        }

        Movie resultMovie = new Movie();
        resultMovie.addTrack(videoTracks);// 视频部分
        resultMovie.addTrack(audioTracks);// 音频部分

        Container out = new DefaultMp4Builder().build(resultMovie);
        FileOutputStream fos = new FileOutputStream(new File(outPath));
        out.writeContainer(fos.getChannel());
        fos.close();
    }


    /**
     * 分离mp4视频的音频部分，只保留视频部分
     *
     * @param mp4Path .mp4
     * @param outPath .mp4
     */
    public static void splitMp4(String mp4Path, String outPath) {

        try {
            Movie videoMovie = MovieCreator.build(mp4Path);
            Track videoTracks = null;// 获取视频的单纯视频部分
            for (Track videoMovieTrack : videoMovie.getTracks()) {
                if ("vide".equals(videoMovieTrack.getHandler())) {
                    videoTracks = videoMovieTrack;
                }
            }

            Movie resultMovie = new Movie();
            resultMovie.addTrack(videoTracks);// 视频部分

            Container out = new DefaultMp4Builder().build(resultMovie);
            FileOutputStream fos = new FileOutputStream(new File(outPath));
            out.writeContainer(fos.getChannel());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * 分离mp4的视频部分，只保留音频部分
     *
     * @param mp4Path .mp4
     * @param outPath .aac
     */
    public static void splitAac(String mp4Path, String outPath) {

        try {
            Movie videoMovie = MovieCreator.build(mp4Path);
            Track videoTracks = null;// 获取音频的单纯视频部分
            for (Track videoMovieTrack : videoMovie.getTracks()) {
                if ("soun".equals(videoMovieTrack.getHandler())) {
                    videoTracks = videoMovieTrack;
                }
            }

            Movie resultMovie = new Movie();
            resultMovie.addTrack(videoTracks);// 音频部分

            Container out = new DefaultMp4Builder().build(resultMovie);
            FileOutputStream fos = new FileOutputStream(new File(outPath));
            out.writeContainer(fos.getChannel());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 分离mp4视频的音频部分，只保留视频部分
     *
     * @param mp4Path .mp4
     * @param mp4OutPath mp4视频输出路径
     * @param aacOutPath aac视频输出路径
     */
    public static void splitVideo(String mp4Path, String mp4OutPath, String aacOutPath) {

        try {
            Movie videoMovie = MovieCreator.build(mp4Path);
            Track videTracks = null;// 获取视频的单纯视频部分
            Track sounTracks = null;// 获取视频的单纯音频部分

            for (Track videoMovieTrack : videoMovie.getTracks()) {
                if ("vide".equals(videoMovieTrack.getHandler())) {
                    videTracks = videoMovieTrack;
                }
                if ("soun".equals(videoMovieTrack.getHandler())) {
                    sounTracks = videoMovieTrack;
                }
            }

            Movie videMovie = new Movie();
            videMovie.addTrack(videTracks);// 视频部分

            Movie sounMovie = new Movie();
            sounMovie.addTrack(sounTracks);// 音频部分

            // 视频部分
            Container videout = new DefaultMp4Builder().build(videMovie);
            FileOutputStream videfos = new FileOutputStream(new File(mp4OutPath));
            videout.writeContainer(videfos.getChannel());
            videfos.close();

            // 音频部分
            Container sounout = new DefaultMp4Builder().build(sounMovie);
            FileOutputStream sounfos = new FileOutputStream(new File(aacOutPath));
            sounout.writeContainer(sounfos.getChannel());
            sounfos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 对 Mp4 添加字幕
     *
     * @param mp4Path .mp4 添加字幕之前
     * @param outPath .mp4 添加字幕之后
     */
    public static void addSubtitles(String mp4Path, String outPath) throws IOException {
        Movie videoMovie = MovieCreator.build(mp4Path);

        TextTrackImpl subTitleEng = new TextTrackImpl();// 实例化文本通道对象
        subTitleEng.getTrackMetaData().setLanguage("eng");// 设置元数据(语言)

        subTitleEng.getSubs().add(new TextTrackImpl.Line(0, 1000, "Five"));// 参数时间毫秒值
        subTitleEng.getSubs().add(new TextTrackImpl.Line(1000, 2000, "Four"));
        subTitleEng.getSubs().add(new TextTrackImpl.Line(2000, 3000, "Three"));
        subTitleEng.getSubs().add(new TextTrackImpl.Line(3000, 4000, "Two"));
        subTitleEng.getSubs().add(new TextTrackImpl.Line(4000, 5000, "one"));
        subTitleEng.getSubs().add(new TextTrackImpl.Line(5001, 5002, " "));// 省略去测试
        videoMovie.addTrack(subTitleEng);// 将字幕通道添加进视频Movie对象中

        Container out = new DefaultMp4Builder().build(videoMovie);
        FileOutputStream fos = new FileOutputStream(new File(outPath));
        out.writeContainer(fos.getChannel());
        fos.close();
    }

    /**
     * 将 MP4 切割
     *
     * @param mp4Path .mp4
     * @param fromSample 起始位置
     * @param toSample 结束位置
     * @param outPath .mp4
     */
    public static void cropMp4(String mp4Path, long fromSample, long toSample, String outPath)
        throws IOException {
        Movie mp4Movie = MovieCreator.build(mp4Path);
        Track videoTracks = null;// 获取视频的单纯视频部分
        for (Track videoMovieTrack : mp4Movie.getTracks()) {
            if ("vide".equals(videoMovieTrack.getHandler())) {
                videoTracks = videoMovieTrack;
            }
        }
        Track audioTracks = null;// 获取视频的单纯音频部分
        for (Track audioMovieTrack : mp4Movie.getTracks()) {
            if ("soun".equals(audioMovieTrack.getHandler())) {
                audioTracks = audioMovieTrack;
            }
        }

        Movie resultMovie = new Movie();
        resultMovie
            .addTrack(new AppendTrack(new CroppedTrack(videoTracks, fromSample, toSample)));// 视频部分
        resultMovie
            .addTrack(new AppendTrack(new CroppedTrack(audioTracks, fromSample, toSample)));// 音频部分

        Container out = new DefaultMp4Builder().build(resultMovie);
        FileOutputStream fos = new FileOutputStream(new File(outPath));
        out.writeContainer(fos.getChannel());
        fos.close();
    }

    public static String getVideoFilePath(String url) {

        if (TextUtils.isEmpty(url) || url.length() < 5)
            return "";

        if (url.substring(0, 4).equalsIgnoreCase("http")) {
        } else
            url = "file://" + url;

        return url;
    }

    public static String convertSecondsToTime(long seconds) {
        String timeStr = null;
        int hour = 0;
        int minute = 0;
        int second = 0;
        if (seconds <= 0)
            return "00:00";
        else {
            minute = (int) seconds / 60;
            if (minute < 60) {
                second = (int) seconds % 60;
                timeStr = unitFormat(minute) + ":" + unitFormat(second);
            } else {
                hour = minute / 60;
                if (hour > 99)
                    return "99:59:59";
                minute = minute % 60;
                second = (int) (seconds - hour * 3600 - minute * 60);
                timeStr = unitFormat(hour) + ":" + unitFormat(minute) + ":" + unitFormat(second);
            }
        }
        return timeStr;
    }

    public static String unitFormat(int i) {
        String retStr = null;
        if (i >= 0 && i < 10)
            retStr = "0" + Integer.toString(i);
        else
            retStr = "" + i;
        return retStr;
    }

    /**
     * 裁剪视频本地路径
     * @param context
     * @param dirName
     * @param fileNamePrefix
     * @return
     */
    public static String getTrimmedVideoPath(Context context, String dirName, String fileNamePrefix) {
        String finalPath = "";
        String dirPath = "";
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            dirPath = context.getExternalCacheDir() + File.separator + dirName; // /mnt/sdcard/Android/data/<package name>/files/...
        } else {
            dirPath = context.getCacheDir() + File.separator + dirName; // /data/data/<package name>/files/...
        }
        File file = new File(dirPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        finalPath = file.getAbsolutePath();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(new Date());
        String outputName = fileNamePrefix + timeStamp + ".mp4";
        finalPath = finalPath + "/" + outputName;
        return finalPath;
    }

    /**
     * 裁剪视频本地目录路径
     */
    public static String getTrimmedVideoDir(Context context, String dirName) {
        String dirPath = "";
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            dirPath = context.getExternalCacheDir() + File.separator
                + dirName; // /mnt/sdcard/Android/data/<package name>/files/...
        } else {
            dirPath = context.getCacheDir() + File.separator
                + dirName; // /data/data/<package name>/files/...
        }
        File file = new File(dirPath);
        if (!file.exists()) {
            file.mkdirs();
        }
        return dirPath;
    }

    public static String saveImageToSD(Bitmap bmp, String dirPath) {
        if (bmp == null) {
            return "";
        }
        File appDir = new File(dirPath);
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file.getAbsolutePath();
    }


    public static String saveImageToSDForEdit(Bitmap bmp, String dirPath, String fileName) {
        if (bmp == null) {
            return "";
        }
        File appDir = new File(dirPath);
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file.getAbsolutePath();
    }

    public static void deleteFile(File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null && files.length > 0) {
                for (int i = 0; i < files.length; ++i) {
                    deleteFile(files[i]);
                }
            }
        }
        f.delete();
    }

    public static String getSaveEditThumbnailDir(Context context) {
        String state = Environment.getExternalStorageState();
        File rootDir =
            state.equals(Environment.MEDIA_MOUNTED) ? context.getExternalCacheDir()
                : context.getCacheDir();
        File folderDir = new File(rootDir.getAbsolutePath() + File.separator + TRIM_PATH + File.separator + THUMB_PATH);
        if (folderDir == null) {
            folderDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + "videoeditor" + File.separator + "picture");
        }
        if (!folderDir.exists() && folderDir.mkdirs()) {

        }
        return folderDir.getAbsolutePath();
    }
}
