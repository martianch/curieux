package com.github.martianch.curieux;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RemoteFileNavigatorTest {

    final String jsonForSingleImage =
            "{\""
            + "id\":36298,"
            + "\"camera_vector\":\"(-0.7996786596322077,0.10995067053193577,-0.5902752674629126)\","
            + "\"site\":5,"
            + "\"imageid\":\"NLA_405141307EDR_F0050104NCAM00534M_\","
            + "\"subframe_rect\":\"(1,1,1024,1024)\","
            + "\"sol\":86,"
            + "\"scale_factor\":1,"
            + "\"camera_model_component_list\":\"(0.795873,0.777822,-2.04002);(-0.796054,0.116464,-0.5939);(-578.167,-1154.8,-299.19);(-1123.67,164.346,685.56);(-0.7969,0.10001,-0.595761);(0.000274,-0.004951,0.021485)\","
            + "\"instrument\":\"NAV_LEFT_A\","
            + "\"url\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405141307EDR_F0050104NCAM00534M_.JPG\","
            + "\"spacecraft_clock\":405141307.148,"
            + "\"attitude\":\"(0.746473,-0.0212997,0.0353686,-0.664133)\","
            + "\"camera_position\":\"(0.795873,0.777822,-2.04002)\","
            + "\"camera_model_type\":\"CAHVOR\","
            + "\"drive\":104,"
            + "\"xyz\":\"(1.37636,0.164033,-0.0887201)\","
            + "\"created_at\":\"2019-07-29T23:24:24.304Z\","
            + "\"updated_at\":\"2019-09-05T23:20:07.821Z\","
            + "\"mission\":\"msl\","
            + "\"extended\":{\"lmst\":\"Sol-00086M16:17:07.006\","
            + "\"bucket\":\"msl-raws\","
            + "\"mast_az\":\"172.142\","
            + "\"mast_el\":\"36.2001\","
            + "\"url_list\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405141307EDR_F0050104NCAM00534M_.JPG\","
            + "\"contributor\":\"Team MSLICE\","
            + "\"filter_name\":null,"
            + "\"sample_type\":\"full\"},"
            + "\"date_taken\":\"2012-11-02T15:18:29.000Z\","
            + "\"date_received\":\"2012-11-03T09:37:03.000Z\","
            + "\"instrument_sort\":7,"
            + "\"sample_type_sort\":1,"
            + "\"is_thumbnail\":false,"
            + "\"title\":\"Sol 86: Left Navigation Camera\","
            + "\"description\":\"This image was taken by Left Navigation Camera onboard NASA's Mars rover Curiosity on Sol 86 (2012-11-02 15:18:29 UTC).\","
            + "\"link\":\"/raw_images/36298\","
            + "\"image_credit\":\"NASA/JPL-Caltech\","
            + "\"https_url\":\"https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405141307EDR_F0050104NCAM00534M_.JPG\""
            + "}";

    String jsonReply88 =
        "{"
        + "\"items\":["
        + "{\"id\":36298,\"camera_vector\":\"(-0.7996786596322077,0.10995067053193577,-0.5902752674629126)\",\"site\":5,\"imageid\":\"NLA_405141307EDR_F0050104NCAM00534M_\",\"subframe_rect\":\"(1,1,1024,1024)\",\"sol\":86,\"scale_factor\":1,\"camera_model_component_list\":\"(0.795873,0.777822,-2.04002);(-0.796054,0.116464,-0.5939);(-578.167,-1154.8,-299.19);(-1123.67,164.346,685.56);(-0.7969,0.10001,-0.595761);(0.000274,-0.004951,0.021485)\",\"instrument\":\"NAV_LEFT_A\",\"url\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405141307EDR_F0050104NCAM00534M_.JPG\",\"spacecraft_clock\":405141307.148,\"attitude\":\"(0.746473,-0.0212997,0.0353686,-0.664133)\",\"camera_position\":\"(0.795873,0.777822,-2.04002)\",\"camera_model_type\":\"CAHVOR\",\"drive\":104,\"xyz\":\"(1.37636,0.164033,-0.0887201)\",\"created_at\":\"2019-07-29T23:24:24.304Z\",\"updated_at\":\"2019-09-05T23:20:07.821Z\",\"mission\":\"msl\",\"extended\":{\"lmst\":\"Sol-00086M16:17:07.006\",\"bucket\":\"msl-raws\",\"mast_az\":\"172.142\",\"mast_el\":\"36.2001\",\"url_list\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405141307EDR_F0050104NCAM00534M_.JPG\",\"contributor\":\"Team MSLICE\",\"filter_name\":null,\"sample_type\":\"full\"},\"date_taken\":\"2012-11-02T15:18:29.000Z\",\"date_received\":\"2012-11-03T09:37:03.000Z\",\"instrument_sort\":7,\"sample_type_sort\":1,\"is_thumbnail\":false,\"title\":\"Sol 86: Left Navigation Camera\",\"description\":\"This image was taken by Left Navigation Camera onboard NASA's Mars rover Curiosity on Sol 86 (2012-11-02 15:18:29 UTC).\",\"link\":\"/raw_images/36298\",\"image_credit\":\"NASA/JPL-Caltech\",\"https_url\":\"https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405141307EDR_F0050104NCAM00534M_.JPG\"},"
        + "{\"id\":36299,\"camera_vector\":\"(-0.8008708154162221,0.12059882358932068,-0.5865678654370928)\",\"site\":5,\"imageid\":\"NRA_405141307EDR_F0050104NCAM00534M_\",\"subframe_rect\":\"(1,1,1024,1024)\",\"sol\":86,\"scale_factor\":1,\"camera_model_component_list\":\"(0.729955,0.359391,-2.03973);(-0.794205,0.128624,-0.593867);(-591.52,-1126.29,-296.872);(-1104.12,179.857,673.314);(-0.741462,0.102244,-0.663147);(0.008154,-0.008592,0.007793)\",\"instrument\":\"NAV_RIGHT_A\",\"url\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NRA_405141307EDR_F0050104NCAM00534M_.JPG\",\"spacecraft_clock\":405141307.148,\"attitude\":\"(0.746473,-0.0212997,0.0353686,-0.664133)\",\"camera_position\":\"(0.729955,0.359391,-2.03973)\",\"camera_model_type\":\"CAHVOR\",\"drive\":104,\"xyz\":\"(1.37636,0.164033,-0.0887201)\",\"created_at\":\"2019-07-29T23:24:24.316Z\",\"updated_at\":\"2019-09-05T23:20:07.830Z\",\"mission\":\"msl\",\"extended\":{\"lmst\":\"Sol-00086M16:17:07.006\",\"bucket\":\"msl-raws\",\"mast_az\":\"171.408\",\"mast_el\":\"35.9375\",\"url_list\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NRA_405141307EDR_F0050104NCAM00534M_.JPG\",\"contributor\":\"Team MSLICE\",\"filter_name\":null,\"sample_type\":\"full\"},\"date_taken\":\"2012-11-02T15:18:29.000Z\",\"date_received\":\"2012-11-03T09:37:00.000Z\",\"instrument_sort\":8,\"sample_type_sort\":1,\"is_thumbnail\":false,\"title\":\"Sol 86: Right Navigation Camera\",\"description\":\"This image was taken by Right Navigation Camera onboard NASA's Mars rover Curiosity on Sol 86 (2012-11-02 15:18:29 UTC).\",\"link\":\"/raw_images/36299\",\"image_credit\":\"NASA/JPL-Caltech\",\"https_url\":\"https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NRA_405141307EDR_F0050104NCAM00534M_.JPG\"},"
        + "{\"id\":36285,\"camera_vector\":\"(0.03083734997745055,0.1637616761601456,-0.9860178351670864)\",\"site\":5,\"imageid\":\"NLA_405140605EDR_D0050104NCAM00531M_\",\"subframe_rect\":\"(1,1,512,512)\",\"sol\":86,\"scale_factor\":2,\"camera_model_component_list\":\"(0.993065,0.439329,-2.03162);(0.0366353,0.157131,-0.98689);(-581.277,204.66,-243.808);(172.983,622.172,-150.874);(0.0201899,0.159236,-0.987025);(0.000274,-0.004951,0.021485)\",\"instrument\":\"NAV_LEFT_A\",\"url\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405140605EDR_D0050104NCAM00531M_.JPG\",\"spacecraft_clock\":405140605.164,\"attitude\":\"(0.746473,-0.0212997,0.0353686,-0.664133)\",\"camera_position\":\"(0.993065,0.439329,-2.03162)\",\"camera_model_type\":\"CAHVOR\",\"drive\":104,\"xyz\":\"(1.37636,0.164033,-0.0887201)\",\"created_at\":\"2019-07-29T23:24:24.140Z\",\"updated_at\":\"2019-09-05T23:20:07.705Z\",\"mission\":\"msl\",\"extended\":{\"lmst\":\"Sol-00086M16:05:43.085\",\"bucket\":\"msl-raws\",\"mast_az\":\"79.0794\",\"mast_el\":\"80.458\",\"url_list\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405140605EDR_D0050104NCAM00531M_.JPG\",\"contributor\":\"Team MSLICE\",\"filter_name\":null,\"sample_type\":\"downsampled\"},\"date_taken\":\"2012-11-02T15:06:46.000Z\",\"date_received\":\"2012-11-03T09:36:50.000Z\",\"instrument_sort\":7,\"sample_type_sort\":3,\"is_thumbnail\":false,\"title\":\"Sol 86: Left Navigation Camera\",\"description\":\"This image was taken by Left Navigation Camera onboard NASA's Mars rover Curiosity on Sol 86 (2012-11-02 15:06:46 UTC).\",\"link\":\"/raw_images/36285\",\"image_credit\":\"NASA/JPL-Caltech\",\"https_url\":\"https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405140605EDR_D0050104NCAM00531M_.JPG\"},"
        + "{\"id\":36283,\"camera_vector\":\"(0.03083734997745055,0.1637616761601456,-0.9860178351670864)\",\"site\":5,\"imageid\":\"NLA_405140592EDR_D0050104NCAM00531M_\",\"subframe_rect\":\"(1,1,512,512)\",\"sol\":86,\"scale_factor\":2,\"camera_model_component_list\":\"(0.993065,0.439329,-2.03162);(0.0366353,0.157131,-0.98689);(-581.277,204.66,-243.808);(172.983,622.172,-150.874);(0.0201899,0.159236,-0.987025);(0.000274,-0.004951,0.021485)\",\"instrument\":\"NAV_LEFT_A\",\"url\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405140592EDR_D0050104NCAM00531M_.JPG\",\"spacecraft_clock\":405140592.072,\"attitude\":\"(0.746473,-0.0212997,0.0353686,-0.664133)\",\"camera_position\":\"(0.993065,0.439329,-2.03162)\",\"camera_model_type\":\"CAHVOR\",\"drive\":104,\"xyz\":\"(1.37636,0.164033,-0.0887201)\",\"created_at\":\"2019-07-29T23:24:24.115Z\",\"updated_at\":\"2019-09-05T23:20:07.686Z\",\"mission\":\"msl\",\"extended\":{\"lmst\":\"Sol-00086M16:05:31.011\",\"bucket\":\"msl-raws\",\"mast_az\":\"79.0794\",\"mast_el\":\"80.458\",\"url_list\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405140592EDR_D0050104NCAM00531M_.JPG\",\"contributor\":\"Team MSLICE\",\"filter_name\":null,\"sample_type\":\"downsampled\"},\"date_taken\":\"2012-11-02T15:06:34.000Z\",\"date_received\":\"2012-11-03T09:36:57.000Z\",\"instrument_sort\":7,\"sample_type_sort\":3,\"is_thumbnail\":false,\"title\":\"Sol 86: Left Navigation Camera\",\"description\":\"This image was taken by Left Navigation Camera onboard NASA's Mars rover Curiosity on Sol 86 (2012-11-02 15:06:34 UTC).\",\"link\":\"/raw_images/36283\",\"image_credit\":\"NASA/JPL-Caltech\",\"https_url\":\"https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405140592EDR_D0050104NCAM00531M_.JPG\"},"
        + "{\"id\":36281,\"camera_vector\":\"(0.03083734997745055,0.1637616761601456,-0.9860178351670864)\",\"site\":5,\"imageid\":\"NLA_405140579EDR_D0050104NCAM00531M_\",\"subframe_rect\":\"(1,1,512,512)\",\"sol\":86,\"scale_factor\":2,\"camera_model_component_list\":\"(0.993065,0.439329,-2.03162);(0.0366353,0.157131,-0.98689);(-581.277,204.66,-243.808);(172.983,622.172,-150.874);(0.0201899,0.159236,-0.987025);(0.000274,-0.004951,0.021485)\",\"instrument\":\"NAV_LEFT_A\",\"url\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405140579EDR_D0050104NCAM00531M_.JPG\",\"spacecraft_clock\":405140579.072,\"attitude\":\"(0.746473,-0.0212997,0.0353686,-0.664133)\",\"camera_position\":\"(0.993065,0.439329,-2.03162)\",\"camera_model_type\":\"CAHVOR\",\"drive\":104,\"xyz\":\"(1.37636,0.164033,-0.0887201)\",\"created_at\":\"2019-07-29T23:24:24.090Z\",\"updated_at\":\"2019-09-05T23:20:07.665Z\",\"mission\":\"msl\",\"extended\":{\"lmst\":\"Sol-00086M16:05:18.045\",\"bucket\":\"msl-raws\",\"mast_az\":\"79.0794\",\"mast_el\":\"80.458\",\"url_list\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405140579EDR_D0050104NCAM00531M_.JPG\",\"contributor\":\"Team MSLICE\",\"filter_name\":null,\"sample_type\":\"downsampled\"},\"date_taken\":\"2012-11-02T15:06:21.000Z\",\"date_received\":\"2012-11-03T09:36:41.000Z\",\"instrument_sort\":7,\"sample_type_sort\":3,\"is_thumbnail\":false,\"title\":\"Sol 86: Left Navigation Camera\",\"description\":\"This image was taken by Left Navigation Camera onboard NASA's Mars rover Curiosity on Sol 86 (2012-11-02 15:06:21 UTC).\",\"link\":\"/raw_images/36281\",\"image_credit\":\"NASA/JPL-Caltech\",\"https_url\":\"https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405140579EDR_D0050104NCAM00531M_.JPG\"},"
        + "{\"id\":36279,\"camera_vector\":\"(0.03083734997745055,0.1637616761601456,-0.9860178351670864)\",\"site\":5,\"imageid\":\"NLA_405140566EDR_D0050104NCAM00531M_\",\"subframe_rect\":\"(1,1,512,512)\",\"sol\":86,\"scale_factor\":2,\"camera_model_component_list\":\"(0.993065,0.439329,-2.03162);(0.0366353,0.157131,-0.98689);(-581.277,204.66,-243.808);(172.983,622.172,-150.874);(0.0201899,0.159236,-0.987025);(0.000274,-0.004951,0.021485)\",\"instrument\":\"NAV_LEFT_A\",\"url\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405140566EDR_D0050104NCAM00531M_.JPG\",\"spacecraft_clock\":405140566.072,\"attitude\":\"(0.746473,-0.0212997,0.0353686,-0.664133)\",\"camera_position\":\"(0.993065,0.439329,-2.03162)\",\"camera_model_type\":\"CAHVOR\",\"drive\":104,\"xyz\":\"(1.37636,0.164033,-0.0887201)\",\"created_at\":\"2019-07-29T23:24:24.065Z\",\"updated_at\":\"2019-09-05T23:20:07.645Z\",\"mission\":\"msl\",\"extended\":{\"lmst\":\"Sol-00086M16:05:05.080\",\"bucket\":\"msl-raws\",\"mast_az\":\"79.0794\",\"mast_el\":\"80.458\",\"url_list\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405140566EDR_D0050104NCAM00531M_.JPG\",\"contributor\":\"Team MSLICE\",\"filter_name\":null,\"sample_type\":\"downsampled\"},\"date_taken\":\"2012-11-02T15:06:07.000Z\",\"date_received\":\"2012-11-03T09:37:00.000Z\",\"instrument_sort\":7,\"sample_type_sort\":3,\"is_thumbnail\":false,\"title\":\"Sol 86: Left Navigation Camera\",\"description\":\"This image was taken by Left Navigation Camera onboard NASA's Mars rover Curiosity on Sol 86 (2012-11-02 15:06:07 UTC).\",\"link\":\"/raw_images/36279\",\"image_credit\":\"NASA/JPL-Caltech\",\"https_url\":\"https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405140566EDR_D0050104NCAM00531M_.JPG\"},"
        + "{\"id\":36277,\"camera_vector\":\"(0.03083734997745055,0.1637616761601456,-0.9860178351670864)\",\"site\":5,\"imageid\":\"NLA_405140553EDR_D0050104NCAM00531M_\",\"subframe_rect\":\"(1,1,512,512)\",\"sol\":86,\"scale_factor\":2,\"camera_model_component_list\":\"(0.993065,0.439329,-2.03162);(0.0366353,0.157131,-0.98689);(-581.277,204.66,-243.808);(172.983,622.172,-150.874);(0.0201899,0.159236,-0.987025);(0.000274,-0.004951,0.021485)\",\"instrument\":\"NAV_LEFT_A\",\"url\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405140553EDR_D0050104NCAM00531M_.JPG\",\"spacecraft_clock\":405140553.072,\"attitude\":\"(0.746473,-0.0212997,0.0353686,-0.664133)\",\"camera_position\":\"(0.993065,0.439329,-2.03162)\",\"camera_model_type\":\"CAHVOR\",\"drive\":104,\"xyz\":\"(1.37636,0.164033,-0.0887201)\",\"created_at\":\"2019-07-29T23:24:24.038Z\",\"updated_at\":\"2019-09-05T23:20:07.628Z\",\"mission\":\"msl\",\"extended\":{\"lmst\":\"Sol-00086M16:04:53.015\",\"bucket\":\"msl-raws\",\"mast_az\":\"79.0794\",\"mast_el\":\"80.458\",\"url_list\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405140553EDR_D0050104NCAM00531M_.JPG\",\"contributor\":\"Team MSLICE\",\"filter_name\":null,\"sample_type\":\"downsampled\"},\"date_taken\":\"2012-11-02T15:05:55.000Z\",\"date_received\":\"2012-11-03T09:36:57.000Z\",\"instrument_sort\":7,\"sample_type_sort\":3,\"is_thumbnail\":false,\"title\":\"Sol 86: Left Navigation Camera\",\"description\":\"This image was taken by Left Navigation Camera onboard NASA's Mars rover Curiosity on Sol 86 (2012-11-02 15:05:55 UTC).\",\"link\":\"/raw_images/36277\",\"image_credit\":\"NASA/JPL-Caltech\",\"https_url\":\"https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405140553EDR_D0050104NCAM00531M_.JPG\"},"
        + "{\"id\":36275,\"camera_vector\":\"(0.03083734997745055,0.1637616761601456,-0.9860178351670864)\",\"site\":5,\"imageid\":\"NLA_405140540EDR_D0050104NCAM00531M_\",\"subframe_rect\":\"(1,1,512,512)\",\"sol\":86,\"scale_factor\":2,\"camera_model_component_list\":\"(0.993065,0.439329,-2.03162);(0.0366353,0.157131,-0.98689);(-581.277,204.66,-243.808);(172.983,622.172,-150.874);(0.0201899,0.159236,-0.987025);(0.000274,-0.004951,0.021485)\",\"instrument\":\"NAV_LEFT_A\",\"url\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405140540EDR_D0050104NCAM00531M_.JPG\",\"spacecraft_clock\":405140540.072,\"attitude\":\"(0.746473,-0.0212997,0.0353686,-0.664133)\",\"camera_position\":\"(0.993065,0.439329,-2.03162)\",\"camera_model_type\":\"CAHVOR\",\"drive\":104,\"xyz\":\"(1.37636,0.164033,-0.0887201)\",\"created_at\":\"2019-07-29T23:24:24.013Z\",\"updated_at\":\"2019-09-05T23:20:07.609Z\",\"mission\":\"msl\",\"extended\":{\"lmst\":\"Sol-00086M16:04:40.050\",\"bucket\":\"msl-raws\",\"mast_az\":\"79.0794\",\"mast_el\":\"80.458\",\"url_list\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405140540EDR_D0050104NCAM00531M_.JPG\",\"contributor\":\"Team MSLICE\",\"filter_name\":null,\"sample_type\":\"downsampled\"},\"date_taken\":\"2012-11-02T15:05:42.000Z\",\"date_received\":\"2012-11-03T09:36:48.000Z\",\"instrument_sort\":7,\"sample_type_sort\":3,\"is_thumbnail\":false,\"title\":\"Sol 86: Left Navigation Camera\",\"description\":\"This image was taken by Left Navigation Camera onboard NASA's Mars rover Curiosity on Sol 86 (2012-11-02 15:05:42 UTC).\",\"link\":\"/raw_images/36275\",\"image_credit\":\"NASA/JPL-Caltech\",\"https_url\":\"https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405140540EDR_D0050104NCAM00531M_.JPG\"},"
        + "{\"id\":36273,\"camera_vector\":\"(0.03083734997745055,0.1637616761601456,-0.9860178351670864)\",\"site\":5,\"imageid\":\"NLA_405140527EDR_D0050104NCAM00531M_\",\"subframe_rect\":\"(1,1,512,512)\",\"sol\":86,\"scale_factor\":2,\"camera_model_component_list\":\"(0.993065,0.439329,-2.03162);(0.0366353,0.157131,-0.98689);(-581.277,204.66,-243.808);(172.983,622.172,-150.874);(0.0201899,0.159236,-0.987025);(0.000274,-0.004951,0.021485)\",\"instrument\":\"NAV_LEFT_A\",\"url\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405140527EDR_D0050104NCAM00531M_.JPG\",\"spacecraft_clock\":405140527.072,\"attitude\":\"(0.746473,-0.0212997,0.0353686,-0.664133)\",\"camera_position\":\"(0.993065,0.439329,-2.03162)\",\"camera_model_type\":\"CAHVOR\",\"drive\":104,\"xyz\":\"(1.37636,0.164033,-0.0887201)\",\"created_at\":\"2019-07-29T23:24:23.989Z\",\"updated_at\":\"2019-09-05T23:20:07.591Z\",\"mission\":\"msl\",\"extended\":{\"lmst\":\"Sol-00086M16:04:27.085\",\"bucket\":\"msl-raws\",\"mast_az\":\"79.0794\",\"mast_el\":\"80.458\",\"url_list\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405140527EDR_D0050104NCAM00531M_.JPG\",\"contributor\":\"Team MSLICE\",\"filter_name\":null,\"sample_type\":\"downsampled\"},\"date_taken\":\"2012-11-02T15:05:28.000Z\",\"date_received\":\"2012-11-03T09:36:49.000Z\",\"instrument_sort\":7,\"sample_type_sort\":3,\"is_thumbnail\":false,\"title\":\"Sol 86: Left Navigation Camera\",\"description\":\"This image was taken by Left Navigation Camera onboard NASA's Mars rover Curiosity on Sol 86 (2012-11-02 15:05:28 UTC).\",\"link\":\"/raw_images/36273\",\"image_credit\":\"NASA/JPL-Caltech\",\"https_url\":\"https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405140527EDR_D0050104NCAM00531M_.JPG\"},"
        + "{\"id\":36271,\"camera_vector\":\"(0.03083734997745055,0.1637616761601456,-0.9860178351670864)\",\"site\":5,\"imageid\":\"NLA_405140513EDR_D0050104NCAM00531M_\",\"subframe_rect\":\"(1,1,512,512)\",\"sol\":86,\"scale_factor\":2,\"camera_model_component_list\":\"(0.993065,0.439329,-2.03162);(0.0366353,0.157131,-0.98689);(-581.277,204.66,-243.808);(172.983,622.172,-150.874);(0.0201899,0.159236,-0.987025);(0.000274,-0.004951,0.021485)\",\"instrument\":\"NAV_LEFT_A\",\"url\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405140513EDR_D0050104NCAM00531M_.JPG\",\"spacecraft_clock\":405140513.916,\"attitude\":\"(0.746473,-0.0212997,0.0353686,-0.664133)\",\"camera_position\":\"(0.993065,0.439329,-2.03162)\",\"camera_model_type\":\"CAHVOR\",\"drive\":104,\"xyz\":\"(1.37636,0.164033,-0.0887201)\",\"created_at\":\"2019-07-29T23:24:23.964Z\",\"updated_at\":\"2019-09-05T23:20:07.572Z\",\"mission\":\"msl\",\"extended\":{\"lmst\":\"Sol-00086M16:04:15.004\",\"bucket\":\"msl-raws\",\"mast_az\":\"79.0794\",\"mast_el\":\"80.458\",\"url_list\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405140513EDR_D0050104NCAM00531M_.JPG\",\"contributor\":\"Team MSLICE\",\"filter_name\":null,\"sample_type\":\"downsampled\"},\"date_taken\":\"2012-11-02T15:05:16.000Z\",\"date_received\":\"2012-11-03T09:36:50.000Z\",\"instrument_sort\":7,\"sample_type_sort\":3,\"is_thumbnail\":false,\"title\":\"Sol 86: Left Navigation Camera\",\"description\":\"This image was taken by Left Navigation Camera onboard NASA's Mars rover Curiosity on Sol 86 (2012-11-02 15:05:16 UTC).\",\"link\":\"/raw_images/36271\",\"image_credit\":\"NASA/JPL-Caltech\",\"https_url\":\"https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405140513EDR_D0050104NCAM00531M_.JPG\"},"
        + "{\"id\":36264,\"camera_vector\":\"(0.37100763726419556,-0.7447001621331119,0.5547747305082992)\",\"site\":5,\"imageid\":\"NLA_405127598EDR_F0050104NCAM00207M_\",\"subframe_rect\":\"(1,1,1024,1024)\",\"sol\":86,\"scale_factor\":1,\"camera_model_component_list\":\"(0.671055,0.340699,-1.91221);(0.36577,-0.749979,0.551114);(1289.83,152.697,265.257);(-97.9234,233.725,1302.08);(0.381066,-0.74399,0.548863);(0.000274,-0.004951,0.021485)\",\"instrument\":\"NAV_LEFT_A\",\"url\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405127598EDR_F0050104NCAM00207M_.JPG\",\"spacecraft_clock\":405127598.616,\"attitude\":\"(0.746376,-0.0240156,0.035206,-0.664159)\",\"camera_position\":\"(0.671055,0.340699,-1.91221)\",\"camera_model_type\":\"CAHVOR\",\"drive\":104,\"xyz\":\"(1.37636,0.164033,-0.0887201)\",\"created_at\":\"2019-07-29T23:24:23.879Z\",\"updated_at\":\"2019-09-05T23:20:07.507Z\",\"mission\":\"msl\",\"extended\":{\"lmst\":\"Sol-00086M12:34:45.018\",\"bucket\":\"msl-raws\",\"mast_az\":\"296.454\",\"mast_el\":\"-33.6721\",\"url_list\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405127598EDR_F0050104NCAM00207M_.JPG\",\"contributor\":\"Team MSLICE\",\"filter_name\":null,\"sample_type\":\"full\"},\"date_taken\":\"2012-11-02T11:30:00.000Z\",\"date_received\":\"2012-11-02T16:51:02.000Z\",\"instrument_sort\":7,\"sample_type_sort\":1,\"is_thumbnail\":false,\"title\":\"Sol 86: Left Navigation Camera\",\"description\":\"This image was taken by Left Navigation Camera onboard NASA's Mars rover Curiosity on Sol 86 (2012-11-02 11:30:00 UTC).\",\"link\":\"/raw_images/36264\",\"image_credit\":\"NASA/JPL-Caltech\",\"https_url\":\"https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405127598EDR_F0050104NCAM00207M_.JPG\"},"
        + "{\"id\":36265,\"camera_vector\":\"(0.36059483975027096,-0.7468713332856078,0.5587079497032906)\",\"site\":5,\"imageid\":\"NRA_405127598EDR_F0050104NCAM00207M_\",\"subframe_rect\":\"(1,1,1024,1024)\",\"sol\":86,\"scale_factor\":1,\"camera_model_component_list\":\"(1.05355,0.522714,-1.9119);(0.354656,-0.75525,0.551181);(1267.94,137.991,282.592);(-112.063,220.714,1281.99);(0.390435,-0.788869,0.474584);(0.008154,-0.008592,0.007793)\",\"instrument\":\"NAV_RIGHT_A\",\"url\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NRA_405127598EDR_F0050104NCAM00207M_.JPG\",\"spacecraft_clock\":405127598.616,\"attitude\":\"(0.746376,-0.0240156,0.035206,-0.664159)\",\"camera_position\":\"(1.05355,0.522714,-1.9119)\",\"camera_model_type\":\"CAHVOR\",\"drive\":104,\"xyz\":\"(1.37636,0.164033,-0.0887201)\",\"created_at\":\"2019-07-29T23:24:23.891Z\",\"updated_at\":\"2019-09-05T23:20:07.516Z\",\"mission\":\"msl\",\"extended\":{\"lmst\":\"Sol-00086M12:34:45.018\",\"bucket\":\"msl-raws\",\"mast_az\":\"295.743\",\"mast_el\":\"-33.9428\",\"url_list\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NRA_405127598EDR_F0050104NCAM00207M_.JPG\",\"contributor\":\"Team MSLICE\",\"filter_name\":null,\"sample_type\":\"full\"},\"date_taken\":\"2012-11-02T11:30:00.000Z\",\"date_received\":\"2012-11-02T16:51:03.000Z\",\"instrument_sort\":8,\"sample_type_sort\":1,\"is_thumbnail\":false,\"title\":\"Sol 86: Right Navigation Camera\",\"description\":\"This image was taken by Right Navigation Camera onboard NASA's Mars rover Curiosity on Sol 86 (2012-11-02 11:30:00 UTC).\",\"link\":\"/raw_images/36265\",\"image_credit\":\"NASA/JPL-Caltech\",\"https_url\":\"https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NRA_405127598EDR_F0050104NCAM00207M_.JPG\"},"
        + "{\"id\":36251,\"camera_vector\":\"(0.37100763726419556,-0.7447001621331119,0.5547747305082992)\",\"site\":5,\"imageid\":\"NLA_405127162EDR_F0050104NCAM00207M_\",\"subframe_rect\":\"(1,1,1024,1024)\",\"sol\":86,\"scale_factor\":1,\"camera_model_component_list\":\"(0.671055,0.340699,-1.91221);(0.36577,-0.749979,0.551114);(1289.83,152.697,265.257);(-97.9234,233.725,1302.08);(0.381066,-0.74399,0.548863);(0.000274,-0.004951,0.021485)\",\"instrument\":\"NAV_LEFT_A\",\"url\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405127162EDR_F0050104NCAM00207M_.JPG\",\"spacecraft_clock\":405127162.667,\"attitude\":\"(0.746376,-0.0240156,0.035206,-0.664159)\",\"camera_position\":\"(0.671055,0.340699,-1.91221)\",\"camera_model_type\":\"CAHVOR\",\"drive\":104,\"xyz\":\"(1.37636,0.164033,-0.0887201)\",\"created_at\":\"2019-07-29T23:24:23.721Z\",\"updated_at\":\"2019-09-05T23:20:07.386Z\",\"mission\":\"msl\",\"extended\":{\"lmst\":\"Sol-00086M12:27:40.089\",\"bucket\":\"msl-raws\",\"mast_az\":\"296.454\",\"mast_el\":\"-33.6721\",\"url_list\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405127162EDR_F0050104NCAM00207M_.JPG\",\"contributor\":\"Team MSLICE\",\"filter_name\":null,\"sample_type\":\"full\"},\"date_taken\":\"2012-11-02T11:22:44.000Z\",\"date_received\":\"2012-11-02T16:50:55.000Z\",\"instrument_sort\":7,\"sample_type_sort\":1,\"is_thumbnail\":false,\"title\":\"Sol 86: Left Navigation Camera\",\"description\":\"This image was taken by Left Navigation Camera onboard NASA's Mars rover Curiosity on Sol 86 (2012-11-02 11:22:44 UTC).\",\"link\":\"/raw_images/36251\",\"image_credit\":\"NASA/JPL-Caltech\",\"https_url\":\"https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405127162EDR_F0050104NCAM00207M_.JPG\"},"
        + "{\"id\":36252,\"camera_vector\":\"(0.36059483975027096,-0.7468713332856078,0.5587079497032906)\",\"site\":5,\"imageid\":\"NRA_405127162EDR_F0050104NCAM00207M_\",\"subframe_rect\":\"(1,1,1024,1024)\",\"sol\":86,\"scale_factor\":1,\"camera_model_component_list\":\"(1.05355,0.522714,-1.9119);(0.354656,-0.75525,0.551181);(1267.94,137.991,282.592);(-112.063,220.714,1281.99);(0.390435,-0.788869,0.474584);(0.008154,-0.008592,0.007793)\",\"instrument\":\"NAV_RIGHT_A\",\"url\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NRA_405127162EDR_F0050104NCAM00207M_.JPG\",\"spacecraft_clock\":405127162.667,\"attitude\":\"(0.746376,-0.0240156,0.035206,-0.664159)\",\"camera_position\":\"(1.05355,0.522714,-1.9119)\",\"camera_model_type\":\"CAHVOR\",\"drive\":104,\"xyz\":\"(1.37636,0.164033,-0.0887201)\",\"created_at\":\"2019-07-29T23:24:23.732Z\",\"updated_at\":\"2019-09-05T23:20:07.395Z\",\"mission\":\"msl\",\"extended\":{\"lmst\":\"Sol-00086M12:27:40.089\",\"bucket\":\"msl-raws\",\"mast_az\":\"295.743\",\"mast_el\":\"-33.9428\",\"url_list\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NRA_405127162EDR_F0050104NCAM00207M_.JPG\",\"contributor\":\"Team MSLICE\",\"filter_name\":null,\"sample_type\":\"full\"},\"date_taken\":\"2012-11-02T11:22:44.000Z\",\"date_received\":\"2012-11-02T16:50:57.000Z\",\"instrument_sort\":8,\"sample_type_sort\":1,\"is_thumbnail\":false,\"title\":\"Sol 86: Right Navigation Camera\",\"description\":\"This image was taken by Right Navigation Camera onboard NASA's Mars rover Curiosity on Sol 86 (2012-11-02 11:22:44 UTC).\",\"link\":\"/raw_images/36252\",\"image_credit\":\"NASA/JPL-Caltech\",\"https_url\":\"https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NRA_405127162EDR_F0050104NCAM00207M_.JPG\"},"
        + "{\"id\":36229,\"camera_vector\":\"(0.37100763726419556,-0.7447001621331119,0.5547747305082992)\",\"site\":5,\"imageid\":\"NLA_405126639EDR_F0050104NCAM00320M_\",\"subframe_rect\":\"(1,1,1024,1024)\",\"sol\":86,\"scale_factor\":1,\"camera_model_component_list\":\"(0.671055,0.340699,-1.91221);(0.36577,-0.749979,0.551114);(1289.83,152.697,265.257);(-97.9234,233.725,1302.08);(0.381066,-0.74399,0.548863);(0.000274,-0.004951,0.021485)\",\"instrument\":\"NAV_LEFT_A\",\"url\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405126639EDR_F0050104NCAM00320M_.JPG\",\"spacecraft_clock\":405126639.557,\"attitude\":\"(0.746376,-0.0240156,0.035206,-0.664159)\",\"camera_position\":\"(0.671055,0.340699,-1.91221)\",\"camera_model_type\":\"CAHVOR\",\"drive\":104,\"xyz\":\"(1.37636,0.164033,-0.0887201)\",\"created_at\":\"2019-07-29T23:24:23.438Z\",\"updated_at\":\"2019-09-05T23:20:07.186Z\",\"mission\":\"msl\",\"extended\":{\"lmst\":\"Sol-00086M12:19:11.078\",\"bucket\":\"msl-raws\",\"mast_az\":\"296.454\",\"mast_el\":\"-33.6721\",\"url_list\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405126639EDR_F0050104NCAM00320M_.JPG\",\"contributor\":\"Team MSLICE\",\"filter_name\":null,\"sample_type\":\"full\"},\"date_taken\":\"2012-11-02T11:14:01.000Z\",\"date_received\":\"2012-11-02T16:50:51.000Z\",\"instrument_sort\":7,\"sample_type_sort\":1,\"is_thumbnail\":false,\"title\":\"Sol 86: Left Navigation Camera\",\"description\":\"This image was taken by Left Navigation Camera onboard NASA's Mars rover Curiosity on Sol 86 (2012-11-02 11:14:01 UTC).\",\"link\":\"/raw_images/36229\",\"image_credit\":\"NASA/JPL-Caltech\",\"https_url\":\"https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405126639EDR_F0050104NCAM00320M_.JPG\"},"
        + "{\"id\":36230,\"camera_vector\":\"(0.36059483975027096,-0.7468713332856078,0.5587079497032906)\",\"site\":5,\"imageid\":\"NRA_405126639EDR_F0050104NCAM00320M_\",\"subframe_rect\":\"(1,1,1024,1024)\",\"sol\":86,\"scale_factor\":1,\"camera_model_component_list\":\"(1.05355,0.522714,-1.9119);(0.354656,-0.75525,0.551181);(1267.94,137.991,282.592);(-112.063,220.714,1281.99);(0.390435,-0.788869,0.474584);(0.008154,-0.008592,0.007793)\",\"instrument\":\"NAV_RIGHT_A\",\"url\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NRA_405126639EDR_F0050104NCAM00320M_.JPG\",\"spacecraft_clock\":405126639.557,\"attitude\":\"(0.746376,-0.0240156,0.035206,-0.664159)\",\"camera_position\":\"(1.05355,0.522714,-1.9119)\",\"camera_model_type\":\"CAHVOR\",\"drive\":104,\"xyz\":\"(1.37636,0.164033,-0.0887201)\",\"created_at\":\"2019-07-29T23:24:23.450Z\",\"updated_at\":\"2019-09-05T23:20:07.196Z\",\"mission\":\"msl\",\"extended\":{\"lmst\":\"Sol-00086M12:19:11.078\",\"bucket\":\"msl-raws\",\"mast_az\":\"295.743\",\"mast_el\":\"-33.9428\",\"url_list\":\"http://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NRA_405126639EDR_F0050104NCAM00320M_.JPG\",\"contributor\":\"Team MSLICE\",\"filter_name\":null,\"sample_type\":\"full\"},\"date_taken\":\"2012-11-02T11:14:01.000Z\",\"date_received\":\"2012-11-02T16:50:49.000Z\",\"instrument_sort\":8,\"sample_type_sort\":1,\"is_thumbnail\":false,\"title\":\"Sol 86: Right Navigation Camera\",\"description\":\"This image was taken by Right Navigation Camera onboard NASA's Mars rover Curiosity on Sol 86 (2012-11-02 11:14:01 UTC).\",\"link\":\"/raw_images/36230\",\"image_credit\":\"NASA/JPL-Caltech\",\"https_url\":\"https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NRA_405126639EDR_F0050104NCAM00320M_.JPG\"}"
        + "],"
        + "\"more\":false,"
        + "\"total\":16,"
        + "\"page\":0,"
        + "\"per_page\":100"
        + "}";
    List<String> listOfKeys88 = Arrays.asList(
        "2012-11-02T11:14:01.000Z^NLA_405126639EDR_F0050104NCAM00320M_",
        "2012-11-02T11:14:01.000Z^NRA_405126639EDR_F0050104NCAM00320M_",
        "2012-11-02T11:22:44.000Z^NLA_405127162EDR_F0050104NCAM00207M_",
        "2012-11-02T11:22:44.000Z^NRA_405127162EDR_F0050104NCAM00207M_",
        "2012-11-02T11:30:00.000Z^NLA_405127598EDR_F0050104NCAM00207M_",
        "2012-11-02T11:30:00.000Z^NRA_405127598EDR_F0050104NCAM00207M_",
        "2012-11-02T15:05:16.000Z^NLA_405140513EDR_D0050104NCAM00531M_",
        "2012-11-02T15:05:28.000Z^NLA_405140527EDR_D0050104NCAM00531M_",
        "2012-11-02T15:05:42.000Z^NLA_405140540EDR_D0050104NCAM00531M_",
        "2012-11-02T15:05:55.000Z^NLA_405140553EDR_D0050104NCAM00531M_",
        "2012-11-02T15:06:07.000Z^NLA_405140566EDR_D0050104NCAM00531M_",
        "2012-11-02T15:06:21.000Z^NLA_405140579EDR_D0050104NCAM00531M_",
        "2012-11-02T15:06:34.000Z^NLA_405140592EDR_D0050104NCAM00531M_",
        "2012-11-02T15:06:46.000Z^NLA_405140605EDR_D0050104NCAM00531M_",
        "2012-11-02T15:18:29.000Z^NLA_405141307EDR_F0050104NCAM00534M_",
        "2012-11-02T15:18:29.000Z^NRA_405141307EDR_F0050104NCAM00534M_"
        );

//    @Test
//    void makeNewTest() {
//    }
//
//    @Test
//    void setFromTest() {
//    }
//
//    @Test
//    void moveWindowTest() {
//    }
//
//    @Test
//    void _numHigherToLoadTest() {
//    }
//
//    @Test
//    void _numLowerToLoadTest() {
//    }
//
//    @Test
//    void toNextTest() {
//    }
//
//    @Test
//    void toPrevTest() {
//    }
//
//    @Test
//    void toFirstLoadedTest() {
//    }
//
//    @Test
//    void toLastLoadedTest() {
//    }
//
//    @Test
//    void getCurrentValueTest() {
//    }
//
//    @Test
//    void getCurrentKeyTest() {
//    }
//
//    @Test
//    void setCurrentKeyTest() {
//    }

    @Test
    void jsonObjToKeyTest() {
        String key = "2012-11-02T15:18:29.000Z^NLA_405141307EDR_F0050104NCAM00534M_";
        assertEquals(key, newRfn().jsonObjToKey((Map)JsonDiy.jsonToDataStructure(jsonForSingleImage)));
    }

//    @Test
//    void _loadFromJsonReplyTest() {
//    }
//
//    @Test
//    void copyTest() {
//    }
//
//    @Test
//    void getCurrentPathTest() {
//    }

    @Test
    void loadFromDataStructureTest() {
        var rfn = newRfn();
        rfn.loadFromDataStructure(JsonDiy.jsonToDataStructure(jsonReply88));
//        for (String s : rfn.nmap.keySet()) {
//            System.out.println(s+",");
//        }
        assertEquals(
                listOfKeys88,
                new ArrayList<>(rfn.nmap.keySet())
        );
    }

    @Test
    void getPathTest() {
        String full_res = "https://mars.jpl.nasa.gov/msl-raw-images/proj/msl/redops/ods/surface/sol/00086/opgs/edr/ncam/NLA_405141307EDR_F0050104NCAM00534M_.JPG";
        assertEquals(full_res, newRfn().getPath((Map)JsonDiy.jsonToDataStructure(jsonForSingleImage)));
    }

//    @Test
//    void _loadInitialTest() {
//    }
//
//    @Test
//    void _loadHigherTest() {
//    }
//
//    @Test
//    void _onLoadResultTest() {
//    }
//
//    @Test
//    void _loadLowerTest() {
//    }
//
//    @Test
//    void _cleanupHigherTest() {
//    }
//
//    @Test
//    void _cleanupLowerTest() {
//    }

    private RemoteFileNavigator newRfn() {
        return new RemoteFileNavigator();
    }
}