package logic.main;

import dom.datatype.Artist;
import dom.datatype.Post;
import logic.master.ArtistLookupMaster;
import logic.master.PostDownloadMaster;
import pers.net.Booru;
import pers.net.Danbooru2;
import pers.net.Gelbooru;
import pers.stor.Configuration;
import pers.stor.datatype.ArtistStorage;
import pers.stor.datatype.PostStorage;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

public class GDPv4 {

    private static ConcurrentHashMap<Booru, List<String>> downloadQueue = new ConcurrentHashMap<>();
    private static ConcurrentLinkedQueue<String> urlQueue = new ConcurrentLinkedQueue<>();
    private static List<Post> postList = new ArrayList<>();
    private static BlockingQueue<String> artistQueue = new LinkedBlockingQueue<>();
    private static List<Artist> artistList = new ArrayList<>();
    private static ExecutorService threadpool = Executors.newCachedThreadPool();

    private GDPv4(){

    }

    public static void setUrls(String urls){
        urlQueue.addAll(Arrays.asList(urls.split("\\r?\\n+")));
    }

    public static void parseUrls(){
        for(Booru b : Configuration.getBoards()){
            for(String url : urlQueue) {
                String postId = "";
                String searchtoken = "";
                Pattern search = null;
                String filtertoken = "";

                if (b instanceof Danbooru2) {
                    searchtoken = "(?:/\\d+)";
                    filtertoken = "/";
                } else if (b instanceof Gelbooru) {
                    searchtoken = "(?:id=\\d+)";
                    filtertoken = "id=";
                }

                if (url.toLowerCase().contains(b.getUrl().getHost().toLowerCase())) {
                    String[] matches = Pattern.compile(searchtoken)
                            .matcher(url)
                            .results()
                            .map(MatchResult::group)
                            .toArray(String[]::new);

                    postId = matches[0].replace(filtertoken, "");

                    downloadQueue.putIfAbsent(b, new ArrayList<>());
                    downloadQueue.get(b).add(postId);
                    urlQueue.remove(url);
                }
            }
        }

        System.out.println(downloadQueue);
    }

    public static void enqueueArtist(String name) throws InterruptedException {
        artistQueue.put(name);
    }

    public static void enqueueArtists(String[] name){
        artistQueue.addAll(Arrays.asList(name));
    }

    public static void download(String urls) throws ExecutionException, InterruptedException {
        setUrls(urls);
        parseUrls();

        PostDownloadMaster dm = new PostDownloadMaster(downloadQueue);
        Future<List<Post>> result = threadpool.submit(dm);

        if(Configuration.isArtistLookupEnabled()){
            ArtistLookupMaster am = new ArtistLookupMaster(artistQueue);
            Future<List<Artist>> arsRes = threadpool.submit(am);
            artistList = arsRes.get();
        }

        postList = result.get();
    }

    public static void shutdown(){
        threadpool.shutdown();
    }

    //temporary method
    public static void saveAll() throws IOException {
        PostStorage ps = PostStorage.getInstance();
        ps.saveAll(postList);
        if(Configuration.isArtistLookupEnabled()){
            ArtistStorage as = ArtistStorage.getInstance();
            as.saveAll(artistList);
        }
    }
}