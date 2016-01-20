package perf;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.lucene.document.DimensionalLatLonField;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CodecReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.LogDocMergePolicy;
import org.apache.lucene.index.SerialMergeScheduler;
import org.apache.lucene.search.DimensionalPointInPolygonQuery;
import org.apache.lucene.search.DimensionalPointInRectQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TotalHitCountCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.Accountables;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.PrintStreamInfoStream;

// javac -cp build/core/classes/java:build/sandbox/classes/java /l/util/src/main/perf/IndexAndSearchOpenStreetMaps.java; java -cp /l/util/src/main:build/core/classes/java:build/sandbox/classes/java perf.IndexAndSearchOpenStreetMaps

public class IndexAndSearchOpenStreetMaps {

  private static void createIndex() throws IOException {

    long t0 = System.nanoTime();

    CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT);

    int BUFFER_SIZE = 1 << 16;     // 64K
    InputStream is = Files.newInputStream(Paths.get("/lucenedata/open-street-maps/latlon.subsetPlusAllLondon.txt"));
    BufferedReader reader = new BufferedReader(new InputStreamReader(is, decoder), BUFFER_SIZE);

    Directory dir = FSDirectory.open(Paths.get("/l/tmp/bkdtest"));
    IndexWriterConfig iwc = new IndexWriterConfig(null);
    iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
    iwc.setMaxBufferedDocs(109630);
    iwc.setRAMBufferSizeMB(IndexWriterConfig.DISABLE_AUTO_FLUSH);
    iwc.setMergePolicy(new LogDocMergePolicy());
    iwc.setMergeScheduler(new SerialMergeScheduler());
    iwc.setInfoStream(new PrintStreamInfoStream(System.out));
    IndexWriter w = new IndexWriter(dir, iwc);
    int count = 0;
    while (true) {
      String line = reader.readLine();
      if (line == null) {
        break;
      }

      String[] parts = line.split(",");
      //long id = Long.parseLong(parts[0]);
      double lat = Double.parseDouble(parts[1]);
      double lon = Double.parseDouble(parts[2]);
      Document doc = new Document();
      doc.add(new DimensionalLatLonField("point", lat, lon));
      w.addDocument(doc);
      count++;
      if (count % 1000000 == 0) {
        System.out.println(count + "...");
      }
    }
    long t1 = System.nanoTime();
    System.out.println(((t1-t0)/1000000000.0) + " sec to index");
    System.out.println(w.maxDoc() + " total docs");
    //System.out.println("Force merge...");
    //w.forceMerge(1);
    long t2 = System.nanoTime();
    //System.out.println(((t2-t1)/1000000000.0) + " sec to force merge");

    w.close();
    long t3 = System.nanoTime();
    System.out.println(((t3-t2)/1000000000.0) + " sec to close");
  }

  private static void queryIndex() throws IOException {
    Directory dir = FSDirectory.open(Paths.get("/l/tmp/bkdtest"));
    System.out.println("DIR: " + dir);
    IndexReader r = DirectoryReader.open(dir);
    long bytes = 0;
    for(LeafReaderContext ctx : r.leaves()) {
      CodecReader cr = (CodecReader) ctx.reader();
      for(Accountable acc : cr.getChildResources()) {
        System.out.println("  " + Accountables.toString(acc));
      }
      bytes += cr.ramBytesUsed();
    }
    System.out.println("READER MB: " + (bytes/1024./1024.));

    System.out.println("maxDoc=" + r.maxDoc());
    IndexSearcher s = new IndexSearcher(r);
    //SegmentReader sr = (SegmentReader) r.leaves().get(0).reader();
    //BKDTreeReader reader = ((BKDTreeSortedNumericDocValues) sr.getSortedNumericDocValues("point")).getBKDTreeReader();

    //System.out.println("reader MB heap=" + (reader.ramBytesUsed()/1024/1024.));

    double[] lats = new double[] {51.30511, 51.30754, 51.31005, 51.31261, 51.31638, 51.31677, 51.31799, 51.31855, 51.3202, 51.31889, 51.3192, 51.32004, 51.32076, 51.32126, 51.32264, 51.32462, 51.32554, 51.32861, 51.33106, 51.33271, 51.33367, 51.33621, 51.3386, 51.33808, 51.33752, 51.33827, 51.33845, 51.33714, 51.3352, 51.33299, 51.33048, 51.33153, 51.33377, 51.33389, 51.33339, 51.32878, 51.32615, 51.3208, 51.31818, 51.3157, 51.31614, 51.31251, 51.31019, 51.30691, 51.30594, 51.30469, 51.30329, 51.2996, 51.2955, 51.29255, 51.29196, 51.29478, 51.29626, 51.29901, 51.30045, 51.30316, 51.30525, 51.30642, 51.30701, 51.30571, 51.30356, 51.30273, 51.30172, 51.29952, 51.2984, 51.29708, 51.29487, 51.29267, 51.29278, 51.29401, 51.2938, 51.29384, 51.29389, 51.29291, 51.29216, 51.2924, 51.29164, 51.291, 51.29029, 51.28944, 51.28992, 51.28988, 51.29013, 51.29043, 51.29101, 51.29142, 51.29168, 51.29171, 51.29178, 51.29168, 51.29198, 51.29266, 51.29346, 51.29616, 51.29774, 51.30014, 51.30293, 51.30596, 51.3083, 51.30963, 51.31107, 51.31273, 51.31535, 51.31765, 51.31896, 51.32007, 51.32289, 51.32471, 51.32599, 51.32689, 51.32752, 51.32796, 51.32882, 51.33034, 51.33093, 51.33214, 51.33543, 51.3413, 51.34386, 51.34466, 51.34584, 51.34569, 51.34519, 51.34493, 51.34417, 51.34488, 51.34548, 51.34598, 51.34624, 51.34668, 51.3473, 51.34912, 51.35073, 51.35183, 51.3533, 51.35486, 51.35851, 51.35934, 51.36009, 51.36066, 51.36129, 51.36199, 51.36285, 51.36711, 51.36832, 51.36969, 51.37381, 51.3754, 51.37766, 51.37909, 51.38109, 51.38367, 51.38593, 51.38657, 51.389, 51.39064, 51.39277, 51.39244, 51.39237, 51.39403, 51.39647, 51.39901, 51.40162, 51.40359, 51.40553, 51.40632, 51.40643, 51.40695, 51.40848, 51.40923, 51.41144, 51.41342, 51.41437, 51.41763, 51.41847, 51.4206, 51.42383, 51.4263, 51.42936, 51.43056, 51.43022, 51.42968, 51.42868, 51.43027, 51.43372, 51.4358, 51.43724, 51.43916, 51.44121, 51.44266, 51.44301, 51.4432, 51.44368, 51.4448, 51.44729, 51.4509, 51.45179, 51.45176, 51.45215, 51.45227, 51.45173, 51.452, 51.4521, 51.45267, 51.45286, 51.45337, 51.45388, 51.45494, 51.45614, 51.45822, 51.46, 51.46147, 51.464, 51.46645, 51.46913, 51.471, 51.47294, 51.47509, 51.47638, 51.47756, 51.47882, 51.48014, 51.48227, 51.48504, 51.4873, 51.4909, 51.49217, 51.49322, 51.49434, 51.49554, 51.49656, 51.49661, 51.49727, 51.49782, 51.49783, 51.4978, 51.49898, 51.50203, 51.50474, 51.50652, 51.50659, 51.50663, 51.50668, 51.50668, 51.50694, 51.50739, 51.50796, 51.51042, 51.51051, 51.51051, 51.51054, 51.5106, 51.51077, 51.51284, 51.51436, 51.51935, 51.51879, 51.52037, 51.52093, 51.52214, 51.5241, 51.52407, 51.52562, 51.52661, 51.52881, 51.52636, 51.52403, 51.521, 51.51848, 51.51774, 51.51782, 51.51786, 51.51779, 51.51814, 51.51807, 51.51865, 51.52294, 51.52469, 51.52957, 51.53295, 51.53299, 51.53293, 51.53421, 51.53455, 51.53511, 51.53875, 51.53911, 51.53976, 51.53999, 51.54066, 51.5408, 51.54107, 51.54275, 51.5458, 51.54741, 51.54856, 51.55099, 51.55258, 51.55275, 51.55398, 51.55461, 51.55538, 51.55775, 51.55906, 51.56055, 51.56219, 51.56374, 51.56563, 51.56515, 51.56458, 51.5681, 51.57135, 51.57156, 51.57164, 51.5717, 51.57236, 51.57276, 51.57308, 51.57378, 51.57388, 51.57398, 51.57413, 51.57806, 51.58148, 51.58507, 51.5871, 51.58869, 51.58899, 51.58911, 51.5894, 51.59152, 51.59447, 51.59638, 51.59879, 51.59976, 51.60052, 51.60128, 51.6016, 51.60235, 51.60275, 51.6032, 51.60332, 51.60395, 51.60462, 51.60485, 51.60651, 51.60741, 51.60746, 51.60747, 51.60794, 51.60806, 51.60818, 51.60829, 51.60916, 51.61228, 51.61548, 51.61777, 51.61913, 51.62013, 51.62292, 51.62412, 51.62609, 51.62933, 51.63068, 51.62881, 51.62736, 51.62671, 51.62622, 51.62525, 51.62511, 51.62517, 51.62487, 51.62454, 51.6255, 51.62569, 51.62562, 51.62447, 51.62326, 51.6218, 51.62261, 51.62334, 51.62321, 51.62361, 51.62243, 51.62053, 51.61983, 51.6175, 51.61612, 51.61555, 51.61256, 51.61491, 51.61248, 51.61054, 51.60629, 51.60552, 51.60556, 51.60642, 51.60702, 51.60496, 51.6066, 51.60765, 51.6103, 51.61352, 51.61338, 51.61395, 51.61468, 51.61578, 51.61652, 51.61678, 51.61634, 51.61681, 51.61751, 51.6184, 51.61735, 51.61678, 51.61573, 51.61752, 51.61924, 51.62108, 51.62363, 51.62511, 51.62582, 51.62785, 51.63094, 51.63438, 51.63596, 51.63729, 51.64055, 51.6403, 51.64105, 51.64143, 51.64165, 51.64164, 51.64202, 51.64353, 51.64272, 51.64334, 51.64376, 51.64528, 51.64651, 51.64623, 51.6517, 51.65281, 51.65323, 51.65982, 51.66031, 51.66075, 51.66121, 51.66167, 51.66313, 51.66357, 51.66677, 51.66802, 51.66834, 51.66882, 51.66906, 51.66939, 51.67054, 51.67098, 51.67523, 51.67794, 51.68088, 51.68129, 51.68208, 51.68248, 51.6827, 51.68311, 51.68344, 51.6834, 51.68346, 51.68357, 51.684, 51.68371, 51.68325, 51.68285, 51.68332, 51.68427, 51.68636, 51.68843, 51.68905, 51.68908, 51.68923, 51.68936, 51.68996, 51.68991, 51.69049, 51.69128, 51.69187, 51.6909, 51.68922, 51.68866, 51.68855, 51.68858, 51.68889, 51.68872, 51.68708, 51.68577, 51.686, 51.68686, 51.68703, 51.68749, 51.68749, 51.6808, 51.67345, 51.66894, 51.66398, 51.66531, 51.66814, 51.66937, 51.66896, 51.6686, 51.66279, 51.66184, 51.65915, 51.65847, 51.65986, 51.65852, 51.65888, 51.6574, 51.65678, 51.65592, 51.65605, 51.65581, 51.65488, 51.64839, 51.64418, 51.64196, 51.64222, 51.64304, 51.64399, 51.64481, 51.64391, 51.64325, 51.64205, 51.6408, 51.63867, 51.63849, 51.63801, 51.63664, 51.63649, 51.63576, 51.63579, 51.63635, 51.63647, 51.63662, 51.63815, 51.6396, 51.63957, 51.63778, 51.63643, 51.63383, 51.63264, 51.63036, 51.63009, 51.62866, 51.62696, 51.62631, 51.62576, 51.62526, 51.62355, 51.62101, 51.61981, 51.61818, 51.61746, 51.61616, 51.61544, 51.61433, 51.61336, 51.61318, 51.61378, 51.61469, 51.61642, 51.61685, 51.6169, 51.61705, 51.61792, 51.61842, 51.61899, 51.61949, 51.61994, 51.61962, 51.61964, 51.61932, 51.61657, 51.61506, 51.6127, 51.61281, 51.61458, 51.61824, 51.62098, 51.62236, 51.62383, 51.62469, 51.62596, 51.62748, 51.63066, 51.63067, 51.63066, 51.63104, 51.63162, 51.63165, 51.63048, 51.62937, 51.6287, 51.62741, 51.62594, 51.62478, 51.62386, 51.6226, 51.6212, 51.62064, 51.61997, 51.61907, 51.61769, 51.61622, 51.61478, 51.61378, 51.61227, 51.6115, 51.61089, 51.60955, 51.60904, 51.60842, 51.6069, 51.60473, 51.60309, 51.60243, 51.6016, 51.60088, 51.60139, 51.60118, 51.59984, 51.59841, 51.59524, 51.59444, 51.59299, 51.59234, 51.59195, 51.59137, 51.591, 51.58948, 51.58854, 51.58714, 51.58552, 51.58431, 51.58287, 51.58152, 51.57943, 51.57868, 51.57722, 51.57679, 51.57609, 51.57565, 51.57535, 51.57449, 51.57381, 51.57201, 51.57058, 51.56885, 51.56756, 51.56647, 51.56598, 51.56474, 51.56377, 51.56233, 51.56151, 51.56099, 51.56068, 51.56047, 51.56027, 51.55924, 51.55878, 51.55785, 51.55693, 51.55539, 51.55483, 51.55152, 51.55151, 51.55082, 51.54993, 51.54808, 51.54728, 51.54669, 51.54618, 51.54616, 51.54536, 51.54516, 51.54445, 51.54327, 51.54172, 51.54126, 51.54063, 51.53897, 51.53829, 51.53736, 51.53577, 51.53447, 51.53418, 51.53357, 51.53266, 51.53247, 51.53207, 51.53129, 51.53006, 51.52833, 51.52691, 51.52577, 51.52448, 51.52151, 51.52092, 51.52046, 51.51942, 51.51792, 51.51644, 51.51526, 51.51429, 51.51341, 51.51213, 51.51141, 51.5112, 51.51019, 51.50914, 51.50864, 51.50814, 51.50784, 51.50748, 51.50702, 51.50646, 51.50559, 51.50548, 51.50507, 51.50435, 51.50383, 51.5034, 51.50297, 51.50245, 51.50194, 51.5012, 51.50082, 51.50098, 51.50038, 51.50035, 51.50017, 51.4995, 51.49861, 51.49835, 51.49774, 51.49746, 51.49682, 51.49597, 51.49565, 51.49527, 51.49475, 51.4941, 51.49368, 51.49295, 51.49283, 51.49266, 51.49205, 51.49137, 51.49082, 51.49055, 51.49022, 51.49001, 51.48982, 51.48943, 51.48902, 51.48848, 51.4886, 51.48859, 51.48786, 51.48734, 51.48685, 51.48678, 51.48627, 51.48611, 51.48575, 51.48405, 51.48126, 51.47856, 51.47578, 51.47391, 51.47157, 51.47012, 51.46935, 51.46922, 51.46919, 51.46809, 51.46705, 51.46735, 51.46755, 51.46768, 51.46775, 51.46778, 51.46773, 51.46757, 51.46643, 51.46443, 51.46271, 51.46283, 51.46299, 51.46239, 51.46231, 51.4619, 51.46178, 51.46174, 51.46177, 51.46181, 51.46168, 51.4613, 51.46136, 51.46139, 51.46123, 51.46103, 51.46082, 51.45961, 51.45866, 51.45848, 51.45791, 51.45789, 51.45784, 51.45743, 51.45713, 51.45674, 51.45536, 51.44899, 51.44786, 51.44466, 51.44195, 51.43999, 51.43815, 51.43951, 51.43493, 51.43251, 51.42969, 51.42891, 51.4307, 51.43098, 51.43209, 51.43233, 51.43103, 51.42775, 51.42657, 51.42496, 51.42291, 51.42304, 51.42349, 51.42354, 51.42332, 51.4229, 51.42233, 51.41902, 51.41649, 51.41437, 51.41222, 51.4104, 51.40812, 51.40806, 51.40877, 51.41108, 51.41201, 51.41168, 51.41069, 51.40945, 51.40756, 51.40618, 51.40439, 51.40357, 51.40249, 51.39962, 51.39606, 51.39274, 51.39155, 51.39237, 51.39367, 51.39092, 51.38894, 51.38861, 51.38743, 51.38619, 51.38436, 51.38252, 51.38111, 51.37667, 51.37477, 51.37356, 51.37064, 51.36586, 51.35993, 51.35662, 51.35335, 51.35033, 51.34728, 51.34335, 51.33975, 51.33563, 51.33277, 51.3304, 51.32909, 51.32752, 51.32664, 51.3269, 51.32774, 51.33038, 51.33215, 51.33459, 51.33675, 51.33892, 51.34138, 51.34335, 51.34529, 51.34813, 51.35025, 51.3522, 51.35399, 51.35607, 51.35792, 51.35999, 51.3617, 51.36243, 51.36377, 51.3648, 51.36541, 51.36623, 51.36726, 51.36815, 51.36852, 51.36927, 51.37024, 51.37118, 51.37227, 51.37364, 51.37736, 51.37919, 51.37917, 51.37877, 51.37941, 51.38018, 51.38006, 51.37585, 51.37443, 51.3709, 51.36774, 51.36691, 51.3668, 51.36517, 51.36194, 51.35945, 51.35738, 51.35533, 51.35307, 51.35153, 51.35032, 51.34765, 51.34559, 51.34201, 51.33895, 51.33652, 51.33175, 51.32985, 51.3323, 51.3382, 51.34019, 51.33983, 51.33983, 51.33986, 51.33991, 51.34189, 51.34345, 51.34327, 51.34109, 51.33862, 51.33574, 51.33229, 51.33006, 51.33027, 51.32799, 51.32324, 51.31958, 51.31761, 51.31596, 51.31389, 51.31099, 51.31041, 51.30669, 51.30566, 51.30138, 51.30039, 51.30003, 51.29967, 51.29976, 51.30078, 51.29822, 51.29831, 51.29598, 51.29445, 51.29119, 51.28788, 51.2875, 51.28975, 51.29103, 51.29185, 51.29321, 51.29629, 51.29773, 51.29979, 51.30152, 51.30246, 51.30421, 51.30511};

    double[] lons = new double[] {-0.0887594, -0.0885092, -0.0834171, -0.0819201, -0.0836182, -0.0813994, -0.079757, -0.0788682, -0.0751468, -0.0700552, -0.062106, -0.0601419, -0.0579075, -0.0549094, -0.0513404, -0.0504715, -0.0479091, -0.0490958, -0.0511326, -0.0490915, -0.0450069, -0.0405343, -0.0376689, -0.0345207, -0.0307074, -0.0277277, -0.0246932, -0.0205845, -0.019188, -0.0172335, -0.0148568, -0.0136316, -0.0106159, -0.0095514, -0.0072375, -0.0005315, 0.0033718, 0.0050591, 0.0056262, 0.0074754, 0.0103204, 0.0091514, 0.0084014, 0.0094533, 0.0100912, 0.0104267, 0.0108085, 0.0121309, 0.0136115, 0.0144863, 0.0192063, 0.0203054, 0.0207665, 0.0229833, 0.023983, 0.0261107, 0.0291096, 0.0311167, 0.033737, 0.0364485, 0.0394021, 0.0411095, 0.0423846, 0.0430618, 0.0441382, 0.0435257, 0.042964, 0.042369, 0.0436616, 0.0457041, 0.0478166, 0.0494364, 0.0509475, 0.0526178, 0.0548933, 0.0560228, 0.0567217, 0.0572097, 0.0576614, 0.0583706, 0.0623833, 0.0639376, 0.0651653, 0.066821, 0.0706534, 0.0722025, 0.0744802, 0.0768397, 0.0785681, 0.0802575, 0.0816923, 0.0838864, 0.0862046, 0.0893723, 0.0894569, 0.0863481, 0.0851147, 0.0842748, 0.0847066, 0.0840693, 0.0841076, 0.0850435, 0.0852631, 0.0872285, 0.0898396, 0.0933233, 0.0984792, 0.1005028, 0.1025236, 0.1043618, 0.1072101, 0.1100849, 0.1151532, 0.1177568, 0.1182473, 0.1202182, 0.1195531, 0.1166538, 0.1180537, 0.1222119, 0.1253166, 0.127109, 0.1316423, 0.134502, 0.136931, 0.1370398, 0.1363654, 0.1366576, 0.1374146, 0.1381188, 0.139466, 0.1402946, 0.1425843, 0.1426929, 0.1437396, 0.1440194, 0.1448418, 0.1450115, 0.1453223, 0.1445748, 0.1445264, 0.144927, 0.1450217, 0.1473325, 0.1509984, 0.152031, 0.149754, 0.1525715, 0.1531072, 0.1509482, 0.1503468, 0.1497606, 0.1504313, 0.1499107, 0.148714, 0.1496519, 0.1480367, 0.1548231, 0.1612871, 0.1604599, 0.158624, 0.1576809, 0.1569083, 0.1552755, 0.1535177, 0.152587, 0.1515999, 0.1506337, 0.1488766, 0.153033, 0.1516039, 0.1497668, 0.1507492, 0.1512748, 0.1546106, 0.1513423, 0.1535078, 0.1534985, 0.1554022, 0.157563, 0.1590594, 0.160642, 0.1638764, 0.1661585, 0.1667477, 0.1671079, 0.1683631, 0.169654, 0.1708764, 0.1724332, 0.1736758, 0.178051, 0.1809763, 0.1836291, 0.1872809, 0.1921282, 0.1935428, 0.1943213, 0.1951014, 0.1961076, 0.1963838, 0.1976119, 0.1987753, 0.1996041, 0.2006238, 0.201142, 0.2024274, 0.2032357, 0.2025665, 0.2028353, 0.204759, 0.2076041, 0.2104863, 0.2107543, 0.2109761, 0.2087858, 0.2114802, 0.211541, 0.2149875, 0.2168721, 0.2164094, 0.2175909, 0.2236758, 0.2167022, 0.2103443, 0.2114115, 0.2120591, 0.2127857, 0.2128244, 0.2138359, 0.2152858, 0.2170979, 0.2194424, 0.2217569, 0.2236004, 0.2258782, 0.2282524, 0.228044, 0.2265725, 0.2265237, 0.2266223, 0.2267768, 0.2272851, 0.231842, 0.2346027, 0.2399771, 0.2418912, 0.2414138, 0.2406156, 0.2405479, 0.2404974, 0.2404588, 0.2404437, 0.2400092, 0.240613, 0.2401674, 0.2449125, 0.244415, 0.2421665, 0.2407974, 0.2427474, 0.246474, 0.2467981, 0.2463958, 0.2506707, 0.2514641, 0.2524561, 0.2532581, 0.2537451, 0.2552893, 0.2565247, 0.2576724, 0.2590195, 0.2595254, 0.2613759, 0.2638406, 0.2656608, 0.2654954, 0.2654393, 0.2696529, 0.2740587, 0.2763121, 0.2854908, 0.2889196, 0.2954957, 0.3006946, 0.3067966, 0.3123126, 0.3168615, 0.3260515, 0.3315619, 0.333814, 0.3336883, 0.3290181, 0.3287616, 0.3279504, 0.3277876, 0.3254883, 0.32253, 0.3220486, 0.3206696, 0.3186619, 0.3153822, 0.3150012, 0.3145836, 0.3141085, 0.3136853, 0.313048, 0.302242, 0.2934548, 0.2885988, 0.2867542, 0.2866569, 0.2866522, 0.2866883, 0.2871228, 0.2871211, 0.2869872, 0.2860291, 0.2855464, 0.2852744, 0.2850653, 0.2824656, 0.2791467, 0.2753722, 0.273245, 0.2715454, 0.2712848, 0.2712345, 0.2712896, 0.2701042, 0.269629, 0.2699311, 0.270125, 0.264494, 0.2600697, 0.2578138, 0.2539827, 0.2541672, 0.2547287, 0.2558371, 0.2561419, 0.256462, 0.2572258, 0.2578527, 0.2580531, 0.2614372, 0.261694, 0.2621841, 0.2642265, 0.2642192, 0.2642831, 0.2644126, 0.2633632, 0.259023, 0.2554792, 0.2521678, 0.2492161, 0.2466995, 0.2400255, 0.2375734, 0.2337476, 0.2281979, 0.222838, 0.2167819, 0.2127379, 0.2095388, 0.2066675, 0.2039197, 0.1993632, 0.1907594, 0.1887802, 0.1862113, 0.1831451, 0.1809915, 0.1809113, 0.1803695, 0.1743752, 0.1680699, 0.1639506, 0.1555175, 0.1498247, 0.137403, 0.1326234, 0.128385, 0.1267895, 0.1241714, 0.120956, 0.1134226, 0.1038072, 0.0964866, 0.0936145, 0.094204, 0.0924777, 0.0896029, 0.0855942, 0.0827858, 0.0789226, 0.0716641, 0.0654623, 0.0617463, 0.0559892, 0.0490342, 0.0480358, 0.0484874, 0.0483077, 0.0495432, 0.0507409, 0.0522997, 0.0532447, 0.0542482, 0.0543638, 0.0532441, 0.0465578, 0.0428857, 0.0407585, 0.0384079, 0.0363469, 0.0340007, 0.030701, 0.0290564, 0.0270219, 0.0239378, 0.0238081, 0.0255467, 0.0247051, 0.0251328, 0.0228387, 0.0167774, 0.01278, 0.0100158, 0.0063834, 0.0029475, 0.0005783, -0.0004826, -0.0040105, -0.0058027, -0.0082847, -0.0079725, -0.0090083, -0.0122861, -0.0117444, -0.011845, -0.0117354, -0.0090902, -0.0089546, -0.0089932, -0.0092504, -0.009725, -0.0112252, -0.0114677, -0.0113148, -0.011147, -0.0110245, -0.0107056, -0.0105923, -0.0106231, -0.0113333, -0.0114383, -0.0107605, -0.0105967, -0.0119451, -0.0209606, -0.0266285, -0.0331196, -0.0371098, -0.0425139, -0.0452396, -0.0466981, -0.047235, -0.0476179, -0.0486064, -0.0511131, -0.055141, -0.0593219, -0.063591, -0.0675646, -0.0724584, -0.0778696, -0.0796547, -0.0798806, -0.0812629, -0.0818291, -0.0839362, -0.0895215, -0.0965607, -0.1010156, -0.1061129, -0.1126368, -0.1178198, -0.121217, -0.1219795, -0.1230649, -0.1312666, -0.1354042, -0.1424085, -0.1479236, -0.1539916, -0.1571509, -0.1574059, -0.1592534, -0.1635216, -0.165198, -0.1753547, -0.1815688, -0.1909697, -0.195777, -0.196639, -0.1995304, -0.2025473, -0.2056592, -0.2081225, -0.2145466, -0.2229035, -0.2279934, -0.2294877, -0.2332685, -0.2362157, -0.239224, -0.2428682, -0.2458579, -0.2493729, -0.2510624, -0.2509159, -0.2514041, -0.2540696, -0.257268, -0.2593159, -0.2610362, -0.2616287, -0.2632291, -0.2647491, -0.2684281, -0.271126, -0.2733789, -0.2737372, -0.2777976, -0.2829359, -0.2872535, -0.2917406, -0.2977432, -0.3001018, -0.3024364, -0.303994, -0.3046099, -0.3091181, -0.3142639, -0.3184299, -0.3214935, -0.3273524, -0.3329953, -0.3341645, -0.3372217, -0.3388571, -0.3443316, -0.3490936, -0.3525513, -0.3560298, -0.3597016, -0.3623997, -0.3662628, -0.3704621, -0.3758619, -0.3804695, -0.3845991, -0.3892254, -0.3964951, -0.400595, -0.4032025, -0.410448, -0.4128845, -0.4166389, -0.4198971, -0.4241434, -0.4267023, -0.429457, -0.4330335, -0.4351129, -0.4368246, -0.439171, -0.4421432, -0.4443541, -0.4459614, -0.4478276, -0.4494115, -0.4553689, -0.4587995, -0.463902, -0.4711074, -0.4756339, -0.4781359, -0.481389, -0.4835535, -0.4861203, -0.4889389, -0.4918748, -0.4931372, -0.4937054, -0.4946303, -0.4960185, -0.497247, -0.4992827, -0.4997121, -0.5002891, -0.498653, -0.4978014, -0.4983626, -0.4999329, -0.5000657, -0.499404, -0.4971762, -0.4966377, -0.4954845, -0.4959185, -0.4965073, -0.4974245, -0.4979573, -0.498517, -0.4985727, -0.4989003, -0.4983468, -0.4991909, -0.4987233, -0.4980587, -0.4972999, -0.4971073, -0.4963276, -0.4967404, -0.4976582, -0.4992152, -0.5004319, -0.5006842, -0.5005219, -0.4993845, -0.4985894, -0.4988495, -0.4995495, -0.4991654, -0.4988393, -0.4983853, -0.4973338, -0.4964309, -0.4953745, -0.4951378, -0.4943694, -0.4911593, -0.4894521, -0.4886237, -0.4892199, -0.4888818, -0.4881195, -0.4873765, -0.4857314, -0.4843083, -0.4843744, -0.4855501, -0.4848322, -0.484102, -0.4845793, -0.4845601, -0.4839413, -0.4828742, -0.4830698, -0.4823416, -0.4814259, -0.4808329, -0.481218, -0.4808662, -0.4793181, -0.47752, -0.4766543, -0.4769986, -0.4767772, -0.4769463, -0.477344, -0.4778819, -0.4826859, -0.4840046, -0.4851093, -0.4867427, -0.4877488, -0.4891553, -0.4893278, -0.4897299, -0.4913297, -0.4915956, -0.4909215, -0.4912162, -0.491721, -0.491804, -0.4927492, -0.4936886, -0.4950469, -0.4954646, -0.4944442, -0.4932666, -0.4913889, -0.4905178, -0.4911738, -0.490839, -0.4904634, -0.4901492, -0.4899488, -0.4896866, -0.4891051, -0.4905002, -0.4908346, -0.4902907, -0.4911415, -0.4909216, -0.4903718, -0.4911397, -0.4920972, -0.4919402, -0.4912197, -0.4897702, -0.4899418, -0.4899915, -0.4895585, -0.4884441, -0.485725, -0.4849055, -0.484262, -0.4835855, -0.4839486, -0.4837963, -0.4831802, -0.4836803, -0.4840533, -0.4848077, -0.4850648, -0.4848628, -0.4851045, -0.4849128, -0.4850827, -0.4855655, -0.4860404, -0.4862436, -0.486455, -0.4873127, -0.4874292, -0.4881517, -0.4888003, -0.4892578, -0.4895507, -0.4888922, -0.48929, -0.4892127, -0.4886217, -0.488693, -0.4892309, -0.4895516, -0.4900444, -0.490492, -0.4908703, -0.4923681, -0.4932841, -0.4948498, -0.4953683, -0.4949352, -0.4958579, -0.496528, -0.4969884, -0.4964537, -0.4974897, -0.4983336, -0.4981772, -0.4983002, -0.4985827, -0.4991706, -0.4990436, -0.4995185, -0.4996918, -0.4991609, -0.4990957, -0.4987137, -0.4988954, -0.500835, -0.503065, -0.5042638, -0.5048744, -0.5053302, -0.506498, -0.5076901, -0.5087589, -0.5091966, -0.5095602, -0.5103751, -0.5083445, -0.5069351, -0.5067099, -0.5064249, -0.5060886, -0.5055448, -0.5043845, -0.5027789, -0.4981372, -0.4954058, -0.4934584, -0.4933581, -0.492623, -0.490502, -0.490306, -0.4899252, -0.4896295, -0.4893215, -0.4890615, -0.4888282, -0.4882179, -0.4856308, -0.4810856, -0.4775032, -0.4767138, -0.4762455, -0.4759975, -0.475167, -0.4736586, -0.4728613, -0.4689006, -0.4685758, -0.4666587, -0.4637132, -0.4608373, -0.4595936, -0.4589018, -0.4615023, -0.4576218, -0.4573193, -0.4554206, -0.4559416, -0.4560741, -0.4464702, -0.4459895, -0.4401259, -0.4361726, -0.431104, -0.4275556, -0.4242143, -0.42145, -0.419009, -0.4179218, -0.4122119, -0.410422, -0.4088063, -0.4065129, -0.4031038, -0.4002706, -0.3974727, -0.3950165, -0.3930436, -0.3913635, -0.3874122, -0.3865709, -0.3862301, -0.3899294, -0.3865019, -0.3816577, -0.3770858, -0.3726395, -0.3666446, -0.3616589, -0.3584943, -0.3553059, -0.3544357, -0.3506187, -0.3480227, -0.3446466, -0.3421532, -0.3398028, -0.3372271, -0.3324847, -0.3292129, -0.3267237, -0.3209387, -0.3177201, -0.3148199, -0.3165455, -0.3168118, -0.3117401, -0.310869, -0.3105409, -0.3096546, -0.3093187, -0.3079937, -0.3125105, -0.3162245, -0.3179495, -0.3185222, -0.3197924, -0.321953, -0.3257661, -0.3293112, -0.3299708, -0.3279442, -0.3288762, -0.3292082, -0.3297187, -0.329113, -0.3305944, -0.330139, -0.3269817, -0.3235504, -0.3196086, -0.31521, -0.3116037, -0.3074917, -0.30495, -0.305381, -0.3046185, -0.3038063, -0.301338, -0.2994527, -0.2960393, -0.2931769, -0.2923075, -0.291209, -0.2896103, -0.2864137, -0.2872374, -0.2874976, -0.2860267, -0.2845964, -0.2834574, -0.2819039, -0.2808025, -0.2790178, -0.2772673, -0.276308, -0.2754211, -0.2738196, -0.2725692, -0.2688751, -0.2624112, -0.2613776, -0.2593593, -0.2545492, -0.2505415, -0.2476915, -0.245529, -0.2449688, -0.2446611, -0.2446091, -0.2450415, -0.2409565, -0.2368044, -0.2317686, -0.2264316, -0.224263, -0.2229812, -0.2225787, -0.2219762, -0.222027, -0.2202661, -0.218955, -0.2180152, -0.2204068, -0.2256798, -0.2298477, -0.2231197, -0.2173307, -0.2126708, -0.2068997, -0.2019031, -0.2016515, -0.2015454, -0.2014524, -0.2013914, -0.1993513, -0.1975827, -0.1961196, -0.1883731, -0.1835547, -0.1785287, -0.1739083, -0.1718867, -0.1631022, -0.1643845, -0.1606257, -0.1618887, -0.1612228, -0.15934, -0.15752, -0.158091, -0.1553767, -0.1570881, -0.1553446, -0.1554058, -0.1502712, -0.1454745, -0.1413948, -0.1403066, -0.1375584, -0.1362995, -0.1344309, -0.131951, -0.1313686, -0.1281442, -0.1254686, -0.1177412, -0.11675, -0.1154472, -0.1147164, -0.1090011, -0.1011299, -0.0978574, -0.0963293, -0.0960582, -0.0909795, -0.0896789, -0.0887594};

    if (false) {
      Query query = new DimensionalPointInPolygonQuery("point", lats, lons);
      TotalHitCountCollector c = new TotalHitCountCollector();
      //long t0 = System.nanoTime();
      s.search(query, c);
      System.out.println("DONE\n\n\n\n");
      System.exit(0);
    }

    // London, UK:
    int STEPS = 5;
    double MIN_LAT = 51.0919106;
    double MAX_LAT = 51.6542719;
    double MIN_LON = -0.3867282;
    double MAX_LON = 0.8492337;
    for(int iter=0;iter<100;iter++) {
      long tStart = System.nanoTime();
      long totHits = 0;
      int queryCount = 0;
      for(int latStep=0;latStep<STEPS;latStep++) {
        double lat = MIN_LAT + latStep * (MAX_LAT - MIN_LAT) / STEPS;
        for(int lonStep=0;lonStep<STEPS;lonStep++) {
          double lon = MIN_LON + lonStep * (MAX_LON - MIN_LON) / STEPS;
          for(int latStepEnd=latStep+1;latStepEnd<=STEPS;latStepEnd++) {
            double latEnd = MIN_LAT + latStepEnd * (MAX_LAT - MIN_LAT) / STEPS;
            for(int lonStepEnd=lonStep+1;lonStepEnd<=STEPS;lonStepEnd++) {
              double lonEnd = MIN_LON + lonStepEnd * (MAX_LON - MIN_LON) / STEPS;

              Query q = new DimensionalPointInRectQuery("point", lat, latEnd, lon, lonEnd);
              TotalHitCountCollector c = new TotalHitCountCollector();
              //long t0 = System.nanoTime();
              s.search(q, c);

              //System.out.println("\nITER: now query lat=" + lat + " latEnd=" + latEnd + " lon=" + lon + " lonEnd=" + lonEnd);
              //Bits hits = reader.intersect(lat, latEnd, lon, lonEnd);
              //System.out.println("  total hits: " + hitCount);
              //totHits += ((FixedBitSet) hits).cardinality();
              //System.out.println("  add tot " + c.getTotalHits());
              totHits += c.getTotalHits();
              queryCount++;
            }
          }
        }
      }

      long tEnd = System.nanoTime();
      System.out.println("ITER: " + iter + " " + ((tEnd-tStart)/1000000000.0) + " sec; totHits=" + totHits + "; " + queryCount + " queries");
    }

    IOUtils.close(r, dir);
  }

  public static void main(String[] args) throws IOException {
    createIndex();
    queryIndex();
  }
}