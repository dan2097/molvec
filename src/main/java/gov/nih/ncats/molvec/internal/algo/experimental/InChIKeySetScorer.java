package gov.nih.ncats.molvec.internal.algo.experimental;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import gov.nih.ncats.common.Tuple;
import gov.nih.ncats.molwitch.Chemical;

public class InChIKeySetScorer implements ResultScorer{
    private static String SPLIT_FILE_PREFIX = "ik"; 
    private boolean compressed=false;
    private int plength = 0;
    
    
    private ConcurrentHashMap<Long,Long> ikeys;

    private static BinaryOperator<Long> bin = (a,b)->a|b;

    private File dir;

    private Set<String> preLoaded = new HashSet<String>();

    private String getPref(String l){
        return l.substring(0, plength);
    }

    private void markRead(String l){
        preLoaded.add(getPref(l));
    }

    private boolean isLoaded(String ik){
        if(plength==0)return true;
        return preLoaded.contains(getPref(ik));
    }
    
    private String getSectionName(String p) {
        if(compressed) {
            return SPLIT_FILE_PREFIX +p + ".txt.gz";
        }else {
            return SPLIT_FILE_PREFIX +p + ".txt";
        }
    }
    
    private void loadSection(String ik) {
        String pref = getPref(ik);
        try {
            loadFile(new File (dir, getSectionName(pref)));
        }catch(Exception e) {
            
        }
        markRead(ik);        
    }


    private void loadFile(File iKeysFile){
        if(compressed){
            try(InputStream fis= new FileInputStream(iKeysFile)){
                try(InputStream in = new GZIPInputStream(fis)){
                    new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8)).lines().parallel()
                    .peek(this::markRead)
                    .map(l->Tuple.of(encodeKey(l), encodeStereoKey(l)))

                    .forEach(t->{
                        ikeys.merge(t.k(), t.v(), bin);
                    });	

                    ;

                }

            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }else{
            try(Stream<String> sf= Files.lines((iKeysFile).toPath())){
                sf.parallel()
                .peek(this::markRead)
                .map(l->Tuple.of(encodeKey(l), encodeStereoKey(l)))

                .forEach(t->{
                    ikeys.merge(t.k(), t.v(), bin);
                });	
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    public InChIKeySetScorer(File iKeysFile, boolean compressed){
        this(iKeysFile, compressed,0);
    }
    
    public InChIKeySetScorer(File iKeysFile, boolean compressed, int plength){
        ikeys=new ConcurrentHashMap<Long,Long>(119803351);
        this.compressed=compressed;
        this.plength=plength;

        if(plength==0) {
            dir=iKeysFile.getParentFile();
            loadFile(iKeysFile);
        }else {
            dir=iKeysFile;
        }
    }
    
//    public void writeAll

    private boolean hasIKey(String ikey){
        
        if(!isLoaded(ikey)) {
            loadSection(ikey);
        }
        
        return this.ikeys.containsKey(encodeKey(ikey));
    }

    private boolean hasIKeyStereo(String ikey){
        if(!isLoaded(ikey)) {
            loadSection(ikey);
        }
        
        return (this.ikeys.getOrDefault(encodeKey(ikey),0l) & encodeStereoKey(ikey))!=0;
    }

    private static long encodeKey(String s){
        //stereo-insensitive
        s=s.split("-")[0];

        int x=s.hashCode();
        int y=(s.substring(2)+"!").hashCode()^x;
        long l = (((long)x) << 32) | (y & 0xffffffffL);
        return l;
    }

    private static long encodeStereoKey(String s){
        //stereo-sensitive
        s=s.split("-")[1];

        int x=s.hashCode();
        int y=(s.substring(2)+"!").hashCode()^x;
        long l = (((long)x) << 32) | (y & 0xffffffffL);
        l=Math.abs(l);

        return (1<<((int) (l/64)));
    }

    @Override
    public double score(Chemical c) {
        try {
            String ikey=c.toInchi().getKey();
            if(hasIKey(ikey)){
                if(hasIKeyStereo(ikey)){
                    return 1;
                }
                return 0.95;
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            //			e.printStackTrace();
        }		
        return 0;
    }

}
