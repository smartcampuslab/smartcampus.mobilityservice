/*******************************************************************************
 * Copyright 2012-2013 Trento RISE
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package eu.trentorise.smartcampus.mobility.test;

import java.text.SimpleDateFormat;

import eu.trentorise.smartcampus.network.JsonUtils;
import it.sayservice.platform.smartplanner.data.message.Itinerary;
import it.sayservice.platform.smartplanner.data.message.Position;
import it.sayservice.platform.smartplanner.data.message.StopId;
import it.sayservice.platform.smartplanner.data.message.TType;
import it.sayservice.platform.smartplanner.data.message.Transport;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertDelay;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertParking;
import it.sayservice.platform.smartplanner.data.message.alerts.AlertType;
import it.sayservice.platform.smartplanner.data.message.alerts.CreatorType;
import it.sayservice.platform.smartplanner.data.message.journey.RecurrentJourney;

/**
 * @author raman
 *
 */
public class ObjectCreator {

	// {"from":{"name":"Via alla Cascata","stopId":{"agencyId":"","id":"","extra":null},"stopCode":"null","lon":"11.150705641951768","lat":"46.070518514274546"},"to":{"name":"Via Dante Alighieri","stopId":{"agencyId":"","id":"","extra":null},"stopCode":"null","lon":"11.040441788031298","lat":"45.888927727271394"},"startime":1418365800000,"endtime":1418368038000,"duration":2273000,"walkingDuration":2273,"leg":[{"legId":"null_null","startime":1418365800000,"endtime":1418367693000,"duration":1893000,"from":{"name":"Via alla Cascata","stopId":null,"stopCode":"null","lon":"11.150705641951768","lat":"46.070518514274546"},"to":{"name":"Rovereto Centro","stopId":{"agencyId":"COMUNE_DI_ROVERETO","id":"Rovereto Centro","extra":null},"stopCode":"null","lon":"11.039122649236566","lat":"45.89220369525941"},"transport":{"type":"CAR","agencyId":"null","routeId":"null","routeShortName":"null","tripId":"null"},"legGeometery":{"length":700,"levels":"null","points":"ucexG{z`cAXNr@`@f@H|@?z@MbAOdASDDF@DAFCBEHFJFB?JBhAFjADP@P???xADbA?L?F?f@Fb@Lr@Dl@E~@ELAN@vBTxAPb@D@LBJDFFDHAFCDEBK@I`ACD?`@AX?j@ATEPE\\Iz@YLEHERGb@CLA|N^j@Db@P|At@hCtAHDr@^p@XBB`@L`BV|AJ`@B\\Bd@DzAH~CRfBp@fAbA^f@\\p@JOl@aALKHCFAXDTJTTLHPJTFXDr@Dn@Lh@Np@Nj@?h@AJCNMRZz@jABDFJBDFHDDFD\\FRRLNLXDTBR?R?R@J@H@?BNBJBJHHDDLBP?F?l@OV?j@LPHDDHFTR|A`APNTLb@Fr@FP?ZD`@D`@JZBXBr@XvAb@f@RRBR@V@V?R@p@JTDnAVh@FD?b@@nABb@RLFJLXh@DJRHRDT@RAx@In@Gb@KRIPMHMJUJWHWRy@DQFMDIHEHAJ?F@HJJTF\\@JBZBNFVJEREPAVAn@CRCNATIJIRSLONWlA_CHOXWJGLITKXCNCP?~@Cx@C`AClEOjAOTIXIp@YXMNGZGnC?H?h@GbEgB`A_@`A@~Bd@hCv@`Dp@pAZJBbAP`BFxCAvFJd@ENETGBA`Ae@dCgAb@KZ?\\RRXd@v@Vp@d@z@PPTLRFV@l@Cn@Mz@M`@Ap@Lz@ZxAn@J\\DPPRR@j@OTCbCWZBdCZd@Hb@C`DEr@Hr@HvA^\\Pp@n@xBlB|B`CpAlA`A|@vA`BrBhDf@dAt@~Ad@~@\\Xf@TtAJz@NtAXlAX|A~@LBVGtA{@ZCZD|@\\zAz@VKz@q@d@e@PMREh@@^Vb@lAf@pCLv@J`AHl@PZNHzA@pAAd@HTFRJZXNhCLnBNhBVdBRv@NPZJ`@FJHDJF\\?\\E?C@CBCF?J@DDBD@BADC@ENj@Nn@`@pAn@x@h@jAFv@Fj@H^`@hARpARZz@b@j@p@PPp@x@T`@f@dAXz@BDBJJd@Hl@FvA@dBB`B`@APFFN?J?FE\\xAHd@BlDENAtAGzAEt@DzAb@d@V^^~EnH\\\\b@\\z@ZdB\\zCl@vBC~@GnDKx@LRFdAh@dAZn@Ft@?jDSn@Cp@LfAd@z@^^Np@Z~AnAfBvBhA`B`@b@nCpBn@Z`Bt@dGxCvK|C~C|A`GdE`CvAj@^p@|@LPLPl@n@Vb@Vp@Fv@DpANj@PPTDVIJML_@La@DKLGl@Gh@Jj@LD@jBp@hB|@`Bl@zAZ|Q|ApFXtEDrE?xD?dCN~ZtGtCfAbE~ArAh@xAVhEXhId@rAFrBVxDVvLArADn@HXFx@TxAt@PNfFpEpCbCHHz@v@xBhCdAv@b@\\|@p@^^VV`@^RRhAdAfBtA|MtIh@^hNfIjBfAvClBzBrBzAbB|ElGt@`Ah@t@~@j@dA^hATpFt@TD`AN~HdAN@xATF?@@F@DBNDRJBBNF~EdDlCfBTPnA~@lAv@v@d@^TBBXLB?LB`@Bv@?z@Cn@A|DGzEId@A~A@d@?xBH`Ev@x@P`ARrA^z@ZDBNHr@^x@l@hAdABBh@p@HJjAfB^l@R^jApClAhDjAfERz@Rv@RxARdCHhAD`@B\\h@bIPvA\\|An@tCzAnHZxAHh@J|@J`BAlACpAGrJAH?zADzARpAZhAv@rBJ`@`@vAPpAHbACxDA`AOxBo@lFIp@CNMrAa@tDMbA_@nDCfA@p@?v@FbBHfBPhFBZPjBf@`Db@rDLrD?nC?vD@t@AbDB~CHnDHhDHrCJtDJrCVnB`@fC\\|Bj@rGXbA`@x@`ArA~@jAv@rA`@z@j@hBr@zBj@|@jB~Bn@v@p@v@RVvAbBzAlB|AbCj@`Az@x@x@d@`@TTLHDvBt@b@TdCjAn@XPH?RFTVJj@B\\LHEBCLF@?pAj@fBbAnAlA|JhM\\`@pAxAbAv@x@TbAPh@J`ALtARrIbApBV\\BxCX`IfA^{Gl@YnBe@DARIDCZOt@]RIpC}@vBs@h@]d@DrAVLDHYrAwEBGp@wBTyBf@E@?l@Kv@@`@JNDHBE^YbAWbAIX"},"alertStrikeList":[],"alertDelayList":[],"alertParkingList":[],"alertRoadList":[],"alertAccidentList":[],"extra":null,"length":0.0},{"legId":"null_null","startime":1418367695000,"endtime":1418368013000,"duration":318000,"from":{"name":"Rovereto Centro","stopId":{"agencyId":"COMUNE_DI_ROVERETO","id":"Rovereto Centro","extra":null},"stopCode":"null","lon":"11.039122649236566","lat":"45.89220369525941"},"to":{"name":"Via Dante Alighieri","stopId":{"agencyId":"","id":"","extra":null},"stopCode":"null","lon":"11.040441788031298","lat":"45.888927727271394"},"transport":{"type":"WALK","agencyId":"null","routeId":"null","routeShortName":"null","tripId":"null"},"legGeometery":{"length":19,"levels":"null","points":"gibwGoakbAHYVcAH?PAb@uAhCZPBL@xCF`@?G{@Ak@DUN?T@^?v@?lA?"},"alertStrikeList":[],"alertDelayList":[],"alertParkingList":[],"alertRoadList":[],"alertAccidentList":[],"extra":null,"length":0.0}],"promoted":false}
	public static Itinerary createCarWithParking() {
		return JsonUtils.toObject("{\"from\":{\"name\":\"Via alla Cascata\",\"stopId\":{\"agencyId\":\"\",\"id\":\"\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.150705641951768\",\"lat\":\"46.070518514274546\"},\"to\":{\"name\":\"Via Dante Alighieri\",\"stopId\":{\"agencyId\":\"\",\"id\":\"\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.040441788031298\",\"lat\":\"45.888927727271394\"},\"startime\":1418365800000,\"endtime\":1418368038000,\"duration\":2273000,\"walkingDuration\":2273,\"leg\":[{\"legId\":\"null_null\",\"startime\":1418365800000,\"endtime\":1418367693000,\"duration\":1893000,\"from\":{\"name\":\"Via alla Cascata\",\"stopId\":null,\"stopCode\":\"null\",\"lon\":\"11.150705641951768\",\"lat\":\"46.070518514274546\"},\"to\":{\"name\":\"Rovereto Centro\",\"stopId\":{\"agencyId\":\"COMUNE_DI_ROVERETO\",\"id\":\"Rovereto Centro\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.039122649236566\",\"lat\":\"45.89220369525941\"},\"transport\":{\"type\":\"CAR\",\"agencyId\":\"null\",\"routeId\":\"null\",\"routeShortName\":\"null\",\"tripId\":\"null\"},\"legGeometery\":{\"length\":700,\"levels\":\"null\",\"points\":\"\"},\"alertStrikeList\":[],\"alertDelayList\":[],\"alertParkingList\":[],\"alertRoadList\":[],\"alertAccidentList\":[],\"extra\":null,\"length\":0.0},{\"legId\":\"null_null\",\"startime\":1418367695000,\"endtime\":1418368013000,\"duration\":318000,\"from\":{\"name\":\"Rovereto Centro\",\"stopId\":{\"agencyId\":\"COMUNE_DI_ROVERETO\",\"id\":\"Rovereto Centro\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.039122649236566\",\"lat\":\"45.89220369525941\"},\"to\":{\"name\":\"Via Dante Alighieri\",\"stopId\":{\"agencyId\":\"\",\"id\":\"\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.040441788031298\",\"lat\":\"45.888927727271394\"},\"transport\":{\"type\":\"WALK\",\"agencyId\":\"null\",\"routeId\":\"null\",\"routeShortName\":\"null\",\"tripId\":\"null\"},\"legGeometery\":{\"length\":19,\"levels\":\"null\",\"points\":\"gibwGoakbAHYVcAH?PAb@uAhCZPBL@xCF`@?G{@Ak@DUN?T@^?v@?lA?\"},\"alertStrikeList\":[],\"alertDelayList\":[],\"alertParkingList\":[],\"alertRoadList\":[],\"alertAccidentList\":[],\"extra\":null,\"length\":0.0}],\"promoted\":false}", Itinerary.class); 
	}

	// {"from":{"name":"Via alla Cascata","stopId":{"agencyId":"","id":"","extra":null},"stopCode":"null","lon":"11.150705641951768","lat":"46.070518514274546"},"to":{"name":"Via Dante Alighieri","stopId":{"agencyId":"","id":"","extra":null},"stopCode":"null","lon":"11.040441788031298","lat":"45.888927727271394"},"startime":1419232256000,"endtime":1419234963000,"duration":2707000,"walkingDuration":964,"leg":[{"legId":"null_null","startime":1419232256000,"endtime":1419232559000,"duration":303000,"from":{"name":"Via alla Cascata","stopId":{"agencyId":"","id":"","extra":null},"stopCode":"null","lon":"11.150705641951768","lat":"46.070518514274546"},"to":{"name":"Povo Alla Cascata","stopId":{"agencyId":"12","id":"2833_12","extra":null},"stopCode":"null","lon":"11.150372","lat":"46.067348"},"transport":{"type":"WALK","agencyId":"null","routeId":"null","routeShortName":"null","tripId":"null"},"legGeometery":{"length":21,"levels":"null","points":"ucexG{z`cAXNr@`@f@H|@?z@MbAOdASDDF@DAFCBEHFJFB?JBhAFjADP@P?"},"alertStrikeList":[],"alertDelayList":[],"alertParkingList":[],"alertRoadList":[],"alertAccidentList":[],"extra":null,"length":0.0},{"legId":"12_0002588962014101520150609","startime":1419232560000,"endtime":1419233280000,"duration":720000,"from":{"name":"Povo Alla Cascata","stopId":{"agencyId":"12","id":"2833_12","extra":null},"stopCode":"null","lon":"11.150372","lat":"46.067348"},"to":{"name":"Piazza Dante Dogana","stopId":{"agencyId":"12","id":"2189_12","extra":null},"stopCode":"null","lon":"11.119949","lat":"46.072592"},"transport":{"type":"BUS","agencyId":"12","routeId":"05R","routeShortName":"5","tripId":"0002588962014101520150609"},"legGeometery":{"length":106,"levels":"null","points":"{odxGoy`cAlE?jAZjCOjGn@F^LH@d@O~FOj@o@LyCm@mA@qA~@{AfBG|@B\\Vt@t@jATX`FjGLh@AfA_BbPMdAYjBQXSJiDZ}BFQHUn@UbCKx@Q`@]Jm@C[Ss@kCqByCi@Wc@Fa@Vw@|@[ZaAv@OQUBKT?ZRXRERLdB~ALf@NdCEh@Ud@mBz@QPElAJh@`@NpC^bFl@xFYVA^PhArBT|@^fCB~@IfBSp@oA`@Q`@EbAFr@Bj@MlAaClHgB`De@h@yAbA_HnCIx@K|@zCdAMv@u@U_APa@Ke@LgBzAsFnCKNGFO^KrAgAjF?RcBjHiBtHIh@Nb@pDpB"},"alertStrikeList":[],"alertDelayList":[],"alertParkingList":[],"alertRoadList":[],"alertAccidentList":[],"extra":null,"length":0.0},{"legId":"null_null","startime":1419233280000,"endtime":1419233339000,"duration":59000,"from":{"name":"Piazza Dante Dogana","stopId":{"agencyId":"12","id":"2189_12","extra":null},"stopCode":"null","lon":"11.119949","lat":"46.072592"},"to":{"name":"Trento FS","stopId":{"agencyId":"5","id":"Trento_5","extra":null},"stopCode":"null","lon":"11.119237","lat":"46.072933"},"transport":{"type":"WALK","agencyId":"null","routeId":"null","routeShortName":"null","tripId":"null"},"legGeometery":{"length":8,"levels":"null","points":"{pexGwzzbA@EUOMI?EECCFYlA"},"alertStrikeList":[],"alertDelayList":[],"alertParkingList":[],"alertRoadList":[],"alertAccidentList":[],"extra":null,"length":0.0},{"legId":"5_R10965","startime":1419233580000,"endtime":1419234360000,"duration":780000,"from":{"name":"Trento FS","stopId":{"agencyId":"5","id":"Trento_5","extra":null},"stopCode":"null","lon":"11.119237","lat":"46.072933"},"to":{"name":"Rovereto FS","stopId":{"agencyId":"5","id":"Rovereto_5","extra":null},"stopCode":"null","lon":"11.033589","lat":"45.890927"},"transport":{"type":"TRAIN","agencyId":"5","routeId":"BV_R1_G","routeShortName":"RG","tripId":"R10965"},"legGeometery":{"length":25,"levels":"null","points":"yrexGevzbA`Y|PbS|DzhAiJ`N}I`a@kl@rTgJvj@PbiB`@hdA~M|yAlTbrArp@p]bDxkArSfj@r@vc@vXzt@dc@`b@dm@zi@dn@nItPbr@~eEtK|b@vRpUpV`VtsBtT"},"alertStrikeList":[],"alertDelayList":[],"alertParkingList":[],"alertRoadList":[],"alertAccidentList":[],"extra":null,"length":0.0},{"legId":"null_null","startime":1419234360000,"endtime":1419234902000,"duration":542000,"from":{"name":"Rovereto FS","stopId":{"agencyId":"5","id":"Rovereto_5","extra":null},"stopCode":"null","lon":"11.033589","lat":"45.890927"},"to":{"name":"Via Dante Alighieri","stopId":{"agencyId":"","id":"","extra":null},"stopCode":"null","lon":"11.040441788031298","lat":"45.888927727271394"},"transport":{"type":"WALK","agencyId":"null","routeId":"null","routeShortName":"null","tripId":"null"},"legGeometery":{"length":42,"levels":"null","points":"{~awGyajbAKIa@[a@YKCGCEEIQCOAQ@ODMDIHIHC?m@@A@CBGBYFIn@GbAg@`@Sr@uF?KBsA@qCPm@UaEAQAUGa@?CG{@Ak@DUN?T@^?v@?lA?"},"alertStrikeList":[],"alertDelayList":[],"alertParkingList":[],"alertRoadList":[],"alertAccidentList":[],"extra":null,"length":0.0}],"promoted":false}
	public static Itinerary createTransit() {
		return JsonUtils.toObject("{\"from\":{\"name\":\"Via alla Cascata\",\"stopId\":{\"agencyId\":\"\",\"id\":\"\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.150705641951768\",\"lat\":\"46.070518514274546\"},\"to\":{\"name\":\"Via Dante Alighieri\",\"stopId\":{\"agencyId\":\"\",\"id\":\"\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.040441788031298\",\"lat\":\"45.888927727271394\"},\"startime\":1419232256000,\"endtime\":1419234963000,\"duration\":2707000,\"walkingDuration\":964,\"leg\":[{\"legId\":\"null_null\",\"startime\":1419232256000,\"endtime\":1419232559000,\"duration\":303000,\"from\":{\"name\":\"Via alla Cascata\",\"stopId\":{\"agencyId\":\"\",\"id\":\"\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.150705641951768\",\"lat\":\"46.070518514274546\"},\"to\":{\"name\":\"Povo Alla Cascata\",\"stopId\":{\"agencyId\":\"12\",\"id\":\"2833_12\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.150372\",\"lat\":\"46.067348\"},\"transport\":{\"type\":\"WALK\",\"agencyId\":\"null\",\"routeId\":\"null\",\"routeShortName\":\"null\",\"tripId\":\"null\"},\"legGeometery\":{\"length\":21,\"levels\":\"null\",\"points\":\"\"},\"alertStrikeList\":[],\"alertDelayList\":[],\"alertParkingList\":[],\"alertRoadList\":[],\"alertAccidentList\":[],\"extra\":null,\"length\":0.0},{\"legId\":\"12_0002588962014101520150609\",\"startime\":1419232560000,\"endtime\":1419233280000,\"duration\":720000,\"from\":{\"name\":\"Povo Alla Cascata\",\"stopId\":{\"agencyId\":\"12\",\"id\":\"2833_12\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.150372\",\"lat\":\"46.067348\"},\"to\":{\"name\":\"Piazza Dante Dogana\",\"stopId\":{\"agencyId\":\"12\",\"id\":\"2189_12\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.119949\",\"lat\":\"46.072592\"},\"transport\":{\"type\":\"BUS\",\"agencyId\":\"12\",\"routeId\":\"05R\",\"routeShortName\":\"5\",\"tripId\":\"0002588962014101520150609\"},\"legGeometery\":{\"length\":106,\"levels\":\"null\",\"points\":\"\"},\"alertStrikeList\":[],\"alertDelayList\":[],\"alertParkingList\":[],\"alertRoadList\":[],\"alertAccidentList\":[],\"extra\":null,\"length\":0.0},{\"legId\":\"null_null\",\"startime\":1419233280000,\"endtime\":1419233339000,\"duration\":59000,\"from\":{\"name\":\"Piazza Dante Dogana\",\"stopId\":{\"agencyId\":\"12\",\"id\":\"2189_12\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.119949\",\"lat\":\"46.072592\"},\"to\":{\"name\":\"Trento FS\",\"stopId\":{\"agencyId\":\"5\",\"id\":\"Trento_5\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.119237\",\"lat\":\"46.072933\"},\"transport\":{\"type\":\"WALK\",\"agencyId\":\"null\",\"routeId\":\"null\",\"routeShortName\":\"null\",\"tripId\":\"null\"},\"legGeometery\":{\"length\":8,\"levels\":\"null\",\"points\":\"\"},\"alertStrikeList\":[],\"alertDelayList\":[],\"alertParkingList\":[],\"alertRoadList\":[],\"alertAccidentList\":[],\"extra\":null,\"length\":0.0},{\"legId\":\"5_R10965\",\"startime\":1419233580000,\"endtime\":1419234360000,\"duration\":780000,\"from\":{\"name\":\"Trento FS\",\"stopId\":{\"agencyId\":\"5\",\"id\":\"Trento_5\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.119237\",\"lat\":\"46.072933\"},\"to\":{\"name\":\"Rovereto FS\",\"stopId\":{\"agencyId\":\"5\",\"id\":\"Rovereto_5\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.033589\",\"lat\":\"45.890927\"},\"transport\":{\"type\":\"TRAIN\",\"agencyId\":\"5\",\"routeId\":\"BV_R1_G\",\"routeShortName\":\"RG\",\"tripId\":\"R10965\"},\"legGeometery\":{\"length\":25,\"levels\":\"null\",\"points\":\"\"},\"alertStrikeList\":[],\"alertDelayList\":[],\"alertParkingList\":[],\"alertRoadList\":[],\"alertAccidentList\":[],\"extra\":null,\"length\":0.0},{\"legId\":\"null_null\",\"startime\":1419234360000,\"endtime\":1419234902000,\"duration\":542000,\"from\":{\"name\":\"Rovereto FS\",\"stopId\":{\"agencyId\":\"5\",\"id\":\"Rovereto_5\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.033589\",\"lat\":\"45.890927\"},\"to\":{\"name\":\"Via Dante Alighieri\",\"stopId\":{\"agencyId\":\"\",\"id\":\"\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.040441788031298\",\"lat\":\"45.888927727271394\"},\"transport\":{\"type\":\"WALK\",\"agencyId\":\"null\",\"routeId\":\"null\",\"routeShortName\":\"null\",\"tripId\":\"null\"},\"legGeometery\":{\"length\":42,\"levels\":\"null\",\"points\":\"\"},\"alertStrikeList\":[],\"alertDelayList\":[],\"alertParkingList\":[],\"alertRoadList\":[],\"alertAccidentList\":[],\"extra\":null,\"length\":0.0}],\"promoted\":false}", Itinerary.class);
	} 

	// {"from":{"name":"Via alla Cascata","stopId":{"agencyId":"","id":"","extra":null},"stopCode":"null","lon":"11.150705641951768","lat":"46.070518514274546"},"to":{"name":"Via Dante Alighieri","stopId":{"agencyId":"","id":"","extra":null},"stopCode":"null","lon":"11.040441788031298","lat":"45.888927727271394"},"startime":1419232256000,"endtime":1419243384000,"duration":11146000,"walkingDuration":10664,"leg":[{"legId":"null_null","startime":1419232256000,"endtime":1419232559000,"duration":303000,"from":{"name":"Via alla Cascata","stopId":{"agencyId":"","id":"","extra":null},"stopCode":"null","lon":"11.150705641951768","lat":"46.070518514274546"},"to":{"name":"Povo Alla Cascata","stopId":{"agencyId":"12","id":"2833_12","extra":null},"stopCode":"null","lon":"11.150372","lat":"46.067348"},"transport":{"type":"WALK","agencyId":"null","routeId":"null","routeShortName":"null","tripId":"null"},"legGeometery":{"length":21,"levels":"null","points":""},"alertStrikeList":[],"alertDelayList":[],"alertParkingList":[],"alertRoadList":[],"alertAccidentList":[],"extra":null,"length":0.0},{"legId":"12_0002588962014101520150609","startime":1419232560000,"endtime":1419233040000,"duration":480000,"from":{"name":"Povo Alla Cascata","stopId":{"agencyId":"12","id":"2833_12","extra":null},"stopCode":"null","lon":"11.150372","lat":"46.067348"},"to":{"name":"Venezia \"Port'aquila\"","stopId":{"agencyId":"12","id":"176_12","extra":null},"stopCode":"null","lon":"11.128221","lat":"46.069865"},"transport":{"type":"BUS","agencyId":"12","routeId":"05R","routeShortName":"5","tripId":"0002588962014101520150609"},"legGeometery":{"length":86,"levels":"null","points":""},"alertStrikeList":[],"alertDelayList":[],"alertParkingList":[],"alertRoadList":[],"alertAccidentList":[],"extra":null,"length":0.0},{"legId":"null_null","startime":1419233040000,"endtime":1419233313000,"duration":273000,"from":{"name":"Venezia \"Port'aquila\"","stopId":{"agencyId":"12","id":"176_12","extra":null},"stopCode":"null","lon":"11.128221","lat":"46.069865"},"to":{"name":"Parco Venezia","stopId":{"agencyId":"BIKE_SHARING_ROVERETO","id":"Parco Venezia","extra":null},"stopCode":"null","lon":"11.1272552","lat":"46.0674473"},"transport":{"type":"WALK","agencyId":"null","routeId":"null","routeShortName":"null","tripId":"null"},"legGeometery":{"length":29,"levels":"null","points":""},"alertStrikeList":[],"alertDelayList":[],"alertParkingList":[],"alertRoadList":[],"alertAccidentList":[],"extra":null,"length":0.0},{"legId":"null_null","startime":1419233314000,"endtime":1419241493000,"duration":8179000,"from":{"name":"Parco Venezia","stopId":{"agencyId":"BIKE_SHARING_ROVERETO","id":"Parco Venezia","extra":null},"stopCode":"null","lon":"11.1272552","lat":"46.0674473"},"to":{"name":"Biki Piazzale Follone - Rovereto","stopId":{"agencyId":"BIKE_SHARING_ROVERETO","id":"Biki Piazzale Follone - Rovereto","extra":null},"stopCode":"null","lon":"11.0593546","lat":"45.91699"},"transport":{"type":"BICYCLE","agencyId":"BIKE_SHARING_ROVERETO","routeId":"null","routeShortName":"null","tripId":"null"},"legGeometery":{"length":527,"levels":"null","points":""},"alertStrikeList":[],"alertDelayList":[],"alertParkingList":[],"alertRoadList":[],"alertAccidentList":[],"extra":null,"length":0.0},{"legId":"null_null","startime":1419233314000,"endtime":1419233364000,"duration":50000,"from":{"name":"Biki Piazzale Follone - Rovereto","stopId":{"agencyId":"BIKE_SHARING_ROVERETO","id":"Biki Piazzale Follone - Rovereto","extra":null},"stopCode":"null","lon":"11.0593546","lat":"45.91699"},"to":{"name":"Parco Venezia","stopId":{"agencyId":"BIKE_SHARING_ROVERETO","id":"Parco Venezia","extra":null},"stopCode":"null","lon":"11.0593546","lat":"45.91699"},"transport":{"type":"WALK","agencyId":"null","routeId":"null","routeShortName":"null","tripId":"null"},"legGeometery":{"length":7,"levels":"null","points":""},"alertStrikeList":[],"alertDelayList":[],"alertParkingList":[],"alertRoadList":[],"alertAccidentList":[],"extra":null,"length":0.0},{"legId":"null_null","startime":1419233314000,"endtime":1419234969000,"duration":1655000,"from":{"name":"Parco Venezia","stopId":{"agencyId":"BIKE_SHARING_ROVERETO","id":"Parco Venezia","extra":null},"stopCode":"null","lon":"11.0593546","lat":"45.91699"},"to":{"name":"Biki Piazzale Follone - Rovereto","stopId":{"agencyId":"BIKE_SHARING_ROVERETO","id":"Biki Piazzale Follone - Rovereto","extra":null},"stopCode":"null","lon":"11.038480997085571","lat":"45.88807194273865"},"transport":{"type":"BICYCLE","agencyId":"BIKE_SHARING_ROVERETO","routeId":"null","routeShortName":"null","tripId":"null"},"legGeometery":{"length":162,"levels":"null","points":""},"alertStrikeList":[],"alertDelayList":[],"alertParkingList":[],"alertRoadList":[],"alertAccidentList":[],"extra":null,"length":0.0},{"legId":"null_null","startime":1419243180000,"endtime":1419243328000,"duration":148000,"from":{"name":"Biki Piazzale Follone - Rovereto","stopId":{"agencyId":"BIKE_SHARING_ROVERETO","id":"Biki Piazzale Follone - Rovereto","extra":null},"stopCode":"null","lon":"11.038480997085571","lat":"45.88807194273865"},"to":{"name":"Via Dante Alighieri","stopId":{"agencyId":"","id":"","extra":null},"stopCode":"null","lon":"11.040441788031298","lat":"45.888927727271394"},"transport":{"type":"WALK","agencyId":"null","routeId":"null","routeShortName":"null","tripId":"null"},"legGeometery":{"length":14,"levels":"null","points":""},"alertStrikeList":[],"alertDelayList":[],"alertParkingList":[],"alertRoadList":[],"alertAccidentList":[],"extra":null,"length":0.0}],"promoted":false}
	public static Itinerary createWithBikeSharing() {
		return JsonUtils.toObject("{\"from\":{\"name\":\"Via alla Cascata\",\"stopId\":{\"agencyId\":\"\",\"id\":\"\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.150705641951768\",\"lat\":\"46.070518514274546\"},\"to\":{\"name\":\"Via Dante Alighieri\",\"stopId\":{\"agencyId\":\"\",\"id\":\"\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.040441788031298\",\"lat\":\"45.888927727271394\"},\"startime\":1419232256000,\"endtime\":1419243384000,\"duration\":11146000,\"walkingDuration\":10664,\"leg\":[{\"legId\":\"null_null\",\"startime\":1419232256000,\"endtime\":1419232559000,\"duration\":303000,\"from\":{\"name\":\"Via alla Cascata\",\"stopId\":{\"agencyId\":\"\",\"id\":\"\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.150705641951768\",\"lat\":\"46.070518514274546\"},\"to\":{\"name\":\"Povo Alla Cascata\",\"stopId\":{\"agencyId\":\"12\",\"id\":\"2833_12\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.150372\",\"lat\":\"46.067348\"},\"transport\":{\"type\":\"WALK\",\"agencyId\":\"null\",\"routeId\":\"null\",\"routeShortName\":\"null\",\"tripId\":\"null\"},\"legGeometery\":{\"length\":21,\"levels\":\"null\",\"points\":\"\"},\"alertStrikeList\":[],\"alertDelayList\":[],\"alertParkingList\":[],\"alertRoadList\":[],\"alertAccidentList\":[],\"extra\":null,\"length\":0.0},{\"legId\":\"12_0002588962014101520150609\",\"startime\":1419232560000,\"endtime\":1419233040000,\"duration\":480000,\"from\":{\"name\":\"Povo Alla Cascata\",\"stopId\":{\"agencyId\":\"12\",\"id\":\"2833_12\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.150372\",\"lat\":\"46.067348\"},\"to\":{\"name\":\"Venezia Portaquila\",\"stopId\":{\"agencyId\":\"12\",\"id\":\"176_12\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.128221\",\"lat\":\"46.069865\"},\"transport\":{\"type\":\"BUS\",\"agencyId\":\"12\",\"routeId\":\"05R\",\"routeShortName\":\"5\",\"tripId\":\"0002588962014101520150609\"},\"legGeometery\":{\"length\":86,\"levels\":\"null\",\"points\":\"\"},\"alertStrikeList\":[],\"alertDelayList\":[],\"alertParkingList\":[],\"alertRoadList\":[],\"alertAccidentList\":[],\"extra\":null,\"length\":0.0},{\"legId\":\"null_null\",\"startime\":1419233040000,\"endtime\":1419233313000,\"duration\":273000,\"from\":{\"name\":\"Venezia Portaquila\",\"stopId\":{\"agencyId\":\"12\",\"id\":\"176_12\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.128221\",\"lat\":\"46.069865\"},\"to\":{\"name\":\"Parco Venezia\",\"stopId\":{\"agencyId\":\"BIKE_SHARING_ROVERETO\",\"id\":\"Parco Venezia\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.1272552\",\"lat\":\"46.0674473\"},\"transport\":{\"type\":\"WALK\",\"agencyId\":\"null\",\"routeId\":\"null\",\"routeShortName\":\"null\",\"tripId\":\"null\"},\"legGeometery\":{\"length\":29,\"levels\":\"null\",\"points\":\"\"},\"alertStrikeList\":[],\"alertDelayList\":[],\"alertParkingList\":[],\"alertRoadList\":[],\"alertAccidentList\":[],\"extra\":null,\"length\":0.0},{\"legId\":\"null_null\",\"startime\":1419233314000,\"endtime\":1419241493000,\"duration\":8179000,\"from\":{\"name\":\"Parco Venezia\",\"stopId\":{\"agencyId\":\"BIKE_SHARING_ROVERETO\",\"id\":\"Parco Venezia\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.1272552\",\"lat\":\"46.0674473\"},\"to\":{\"name\":\"Biki Piazzale Follone - Rovereto\",\"stopId\":{\"agencyId\":\"BIKE_SHARING_ROVERETO\",\"id\":\"Biki Piazzale Follone - Rovereto\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.0593546\",\"lat\":\"45.91699\"},\"transport\":{\"type\":\"BICYCLE\",\"agencyId\":\"BIKE_SHARING_ROVERETO\",\"routeId\":\"null\",\"routeShortName\":\"null\",\"tripId\":\"null\"},\"legGeometery\":{\"length\":527,\"levels\":\"null\",\"points\":\"\"},\"alertStrikeList\":[],\"alertDelayList\":[],\"alertParkingList\":[],\"alertRoadList\":[],\"alertAccidentList\":[],\"extra\":null,\"length\":0.0},{\"legId\":\"null_null\",\"startime\":1419233314000,\"endtime\":1419233364000,\"duration\":50000,\"from\":{\"name\":\"Biki Piazzale Follone - Rovereto\",\"stopId\":{\"agencyId\":\"BIKE_SHARING_ROVERETO\",\"id\":\"Biki Piazzale Follone - Rovereto\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.0593546\",\"lat\":\"45.91699\"},\"to\":{\"name\":\"Parco Venezia\",\"stopId\":{\"agencyId\":\"BIKE_SHARING_ROVERETO\",\"id\":\"Parco Venezia\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.0593546\",\"lat\":\"45.91699\"},\"transport\":{\"type\":\"WALK\",\"agencyId\":\"null\",\"routeId\":\"null\",\"routeShortName\":\"null\",\"tripId\":\"null\"},\"legGeometery\":{\"length\":7,\"levels\":\"null\",\"points\":\"\"},\"alertStrikeList\":[],\"alertDelayList\":[],\"alertParkingList\":[],\"alertRoadList\":[],\"alertAccidentList\":[],\"extra\":null,\"length\":0.0},{\"legId\":\"null_null\",\"startime\":1419233314000,\"endtime\":1419234969000,\"duration\":1655000,\"from\":{\"name\":\"Parco Venezia\",\"stopId\":{\"agencyId\":\"BIKE_SHARING_ROVERETO\",\"id\":\"Parco Venezia\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.0593546\",\"lat\":\"45.91699\"},\"to\":{\"name\":\"Biki Piazzale Follone - Rovereto\",\"stopId\":{\"agencyId\":\"BIKE_SHARING_ROVERETO\",\"id\":\"Biki Piazzale Follone - Rovereto\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.038480997085571\",\"lat\":\"45.88807194273865\"},\"transport\":{\"type\":\"BICYCLE\",\"agencyId\":\"BIKE_SHARING_ROVERETO\",\"routeId\":\"null\",\"routeShortName\":\"null\",\"tripId\":\"null\"},\"legGeometery\":{\"length\":162,\"levels\":\"null\",\"points\":\"\"},\"alertStrikeList\":[],\"alertDelayList\":[],\"alertParkingList\":[],\"alertRoadList\":[],\"alertAccidentList\":[],\"extra\":null,\"length\":0.0},{\"legId\":\"null_null\",\"startime\":1419243180000,\"endtime\":1419243328000,\"duration\":148000,\"from\":{\"name\":\"Biki Piazzale Follone - Rovereto\",\"stopId\":{\"agencyId\":\"BIKE_SHARING_ROVERETO\",\"id\":\"Biki Piazzale Follone - Rovereto\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.038480997085571\",\"lat\":\"45.88807194273865\"},\"to\":{\"name\":\"Via Dante Alighieri\",\"stopId\":{\"agencyId\":\"\",\"id\":\"\",\"extra\":null},\"stopCode\":\"null\",\"lon\":\"11.040441788031298\",\"lat\":\"45.888927727271394\"},\"transport\":{\"type\":\"WALK\",\"agencyId\":\"null\",\"routeId\":\"null\",\"routeShortName\":\"null\",\"tripId\":\"null\"},\"legGeometery\":{\"length\":14,\"levels\":\"null\",\"points\":\"\"},\"alertStrikeList\":[],\"alertDelayList\":[],\"alertParkingList\":[],\"alertRoadList\":[],\"alertAccidentList\":[],\"extra\":null,\"length\":0.0}],\"promoted\":false}", Itinerary.class);
	} 
	
	/**
	 * @param i
	 * @return
	 * @throws Exception 
	 */
	public static AlertDelay createTrainDelayForSingle(int i) throws Exception {
		AlertDelay delay = new AlertDelay();
		delay.setCreatorId("1");
		delay.setCreatorType(CreatorType.SERVICE);
		delay.setDelay(i*60*1000);
		delay.setType(AlertType.DELAY);
		
		Transport t = new Transport();
		t.setAgencyId("5");
		t.setRouteId("BV_R1_G");
		t.setRouteShortName("");
		t.setTripId("R10965");
		t.setType(TType.TRAIN);
		delay.setTransport(t);
		delay.setFrom(new SimpleDateFormat("ddMMyyyy").parse("22122014").getTime());
		delay.setTo(new SimpleDateFormat("ddMMyyyy").parse("23122014").getTime());

		Position p = new Position();
		p.setName("Trento FS");
		delay.setPosition(p);
		
		delay.setId(delay.getTransport().getTripId() + "_" + CreatorType.SERVICE + "_" + delay.getFrom() + "_" + delay.getTo());

		return delay;
	}	
	
	
	
	// {"parameters":{"recurrence":[1,2,3,4,5,6,7],"from":{"name":null,"stopId":null,"stopCode":null,"lon":"11.150704","lat":"46.070519"},"to":{"name":null,"stopId":null,"stopCode":null,"lon":"11.040570","lat":"45.888927"},"fromDate":1419268490172,"toDate":1419318555581,"time":"06:14PM","interval":7200000,"transportTypes":["TRANSIT","BICYCLE"],"routeType":"fastest","resultsNumber":3},"legs":[{"from":"Povo Alla Cascata","to":"Piazza Dante Dogana","transport":{"type":"BUS","agencyId":"12","routeId":"05R","routeShortName":"5","tripId":"0002588832014101520150609"}},{"from":"Trento FS","to":"Rovereto FS","transport":{"type":"TRAIN","agencyId":"5","routeId":"BV_R1_G","routeShortName":"RG","tripId":"R20931"}},{"from":"Piazzale Orsi Stazione Fs","to":"Via Dante Borgo S.Caterina","transport":{"type":"BUS","agencyId":"16","routeId":"AA_Rov","routeShortName":"A","tripId":"0002579032014091020150609"}},{"from":"Povo Alla Cascata","to":"Piazza Dante Dogana","transport":{"type":"BUS","agencyId":"12","routeId":"05R","routeShortName":"5","tripId":"0002588822014101520150609"}},{"from":"Trento FS","to":"Rovereto FS","transport":{"type":"TRAIN","agencyId":"5","routeId":"BV_R1_G","routeShortName":"RG","tripId":"RV2265"}},{"from":"Povo Fac. Scienze","to":"Piazza Dante Dogana","transport":{"type":"BUS","agencyId":"12","routeId":"05R","routeShortName":"5","tripId":"0002534452014091020150609"}},{"from":"Trento FS","to":"Rovereto FS","transport":{"type":"TRAIN","agencyId":"5","routeId":"BV_R1_G","routeShortName":"RG","tripId":"R10987"}},{"from":"Piazzale Orsi Stazione Fs","to":"Corso Rosmini Monumento","transport":{"type":"BUS","agencyId":"16","routeId":"01R_Rov","routeShortName":"1","tripId":"0002579352014091020150609"}},{"from":"Povo Piazza Manci","to":"Verona Einaudi","transport":{"type":"BUS","agencyId":"12","routeId":"13R","routeShortName":"13","tripId":"0002551012014091020150609"}},{"from":"Trento-S.Bartolameo","to":"Rovereto-Via Manzoni","transport":{"type":"BUS","agencyId":"17","routeId":"110_17_0","routeShortName":"301","tripId":"0001767532014091020150626"}},{"from":"Povo Alla Cascata","to":"Piazza Dante Dogana","transport":{"type":"BUS","agencyId":"12","routeId":"05R","routeShortName":"5","tripId":"0002551582014091020150609"}},{"from":"Trento FS","to":"Rovereto FS","transport":{"type":"TRAIN","agencyId":"5","routeId":"BV_R1_G","routeShortName":"RG","tripId":"EC83"}},{"from":"Povo Fac. Scienze","to":"Piazza Dante Dogana","transport":{"type":"BUS","agencyId":"12","routeId":"05R","routeShortName":"5","tripId":"0002534672014091020150609"}},{"from":"Trento FS","to":"Rovereto FS","transport":{"type":"TRAIN","agencyId":"5","routeId":"BV_R1_G","routeShortName":"RG","tripId":"R20933"}},{"from":"Povo Piazza Manci","to":"Verona Einaudi","transport":{"type":"BUS","agencyId":"12","routeId":"13R","routeShortName":"13","tripId":"0002551112014091020150609"}},{"from":"Trento-S.Bartolameo","to":"Rovereto-Via Manzoni","transport":{"type":"BUS","agencyId":"17","routeId":"110_17_0","routeShortName":"301","tripId":"0001816952014091020150626"}},{"from":"Povo Fac. Scienze","to":"Piazza Dante Dogana","transport":{"type":"BUS","agencyId":"12","routeId":"05R","routeShortName":"5","tripId":"0002534572014091020150609"}},{"from":"Via alla Cascata","to":"Via Europa","transport":{"type":"BICYCLE","agencyId":"null","routeId":"null","routeShortName":"null","tripId":"null"}}],"monitorLegs":{"12_05R":true,"12_13R":true,"16_01R_Rov":true,"16_AA_Rov":true,"17_110_17_0":true,"5_BV_R1_G":true}}
	public static RecurrentJourney createRecurrent() {
		return JsonUtils.toObject("{\"parameters\":{\"recurrence\":[1,2,3,4,5,6,7],\"from\":{\"name\":null,\"stopId\":null,\"stopCode\":null,\"lon\":\"11.150704\",\"lat\":\"46.070519\"},\"to\":{\"name\":null,\"stopId\":null,\"stopCode\":null,\"lon\":\"11.040570\",\"lat\":\"45.888927\"},\"fromDate\":1419268490172,\"toDate\":1419318555581,\"time\":\"06:14PM\",\"interval\":7200000,\"transportTypes\":[\"TRANSIT\",\"BICYCLE\"],\"routeType\":\"fastest\",\"resultsNumber\":3},\"legs\":[{\"from\":\"Povo Alla Cascata\",\"to\":\"Piazza Dante Dogana\",\"transport\":{\"type\":\"BUS\",\"agencyId\":\"12\",\"routeId\":\"05R\",\"routeShortName\":\"5\",\"tripId\":\"0002588832014101520150609\"}},{\"from\":\"Trento FS\",\"to\":\"Rovereto FS\",\"transport\":{\"type\":\"TRAIN\",\"agencyId\":\"5\",\"routeId\":\"BV_R1_G\",\"routeShortName\":\"RG\",\"tripId\":\"R20931\"}},{\"from\":\"Piazzale Orsi Stazione Fs\",\"to\":\"Via Dante Borgo S.Caterina\",\"transport\":{\"type\":\"BUS\",\"agencyId\":\"16\",\"routeId\":\"AA_Rov\",\"routeShortName\":\"A\",\"tripId\":\"0002579032014091020150609\"}},{\"from\":\"Povo Alla Cascata\",\"to\":\"Piazza Dante Dogana\",\"transport\":{\"type\":\"BUS\",\"agencyId\":\"12\",\"routeId\":\"05R\",\"routeShortName\":\"5\",\"tripId\":\"0002588822014101520150609\"}},{\"from\":\"Trento FS\",\"to\":\"Rovereto FS\",\"transport\":{\"type\":\"TRAIN\",\"agencyId\":\"5\",\"routeId\":\"BV_R1_G\",\"routeShortName\":\"RG\",\"tripId\":\"RV2265\"}},{\"from\":\"Povo Fac. Scienze\",\"to\":\"Piazza Dante Dogana\",\"transport\":{\"type\":\"BUS\",\"agencyId\":\"12\",\"routeId\":\"05R\",\"routeShortName\":\"5\",\"tripId\":\"0002534452014091020150609\"}},{\"from\":\"Trento FS\",\"to\":\"Rovereto FS\",\"transport\":{\"type\":\"TRAIN\",\"agencyId\":\"5\",\"routeId\":\"BV_R1_G\",\"routeShortName\":\"RG\",\"tripId\":\"R10987\"}},{\"from\":\"Piazzale Orsi Stazione Fs\",\"to\":\"Corso Rosmini Monumento\",\"transport\":{\"type\":\"BUS\",\"agencyId\":\"16\",\"routeId\":\"01R_Rov\",\"routeShortName\":\"1\",\"tripId\":\"0002579352014091020150609\"}},{\"from\":\"Povo Piazza Manci\",\"to\":\"Verona Einaudi\",\"transport\":{\"type\":\"BUS\",\"agencyId\":\"12\",\"routeId\":\"13R\",\"routeShortName\":\"13\",\"tripId\":\"0002551012014091020150609\"}},{\"from\":\"Trento-S.Bartolameo\",\"to\":\"Rovereto-Via Manzoni\",\"transport\":{\"type\":\"BUS\",\"agencyId\":\"17\",\"routeId\":\"110_17_0\",\"routeShortName\":\"301\",\"tripId\":\"0001767532014091020150626\"}},{\"from\":\"Povo Alla Cascata\",\"to\":\"Piazza Dante Dogana\",\"transport\":{\"type\":\"BUS\",\"agencyId\":\"12\",\"routeId\":\"05R\",\"routeShortName\":\"5\",\"tripId\":\"0002551582014091020150609\"}},{\"from\":\"Trento FS\",\"to\":\"Rovereto FS\",\"transport\":{\"type\":\"TRAIN\",\"agencyId\":\"5\",\"routeId\":\"BV_R1_G\",\"routeShortName\":\"RG\",\"tripId\":\"EC83\"}},{\"from\":\"Povo Fac. Scienze\",\"to\":\"Piazza Dante Dogana\",\"transport\":{\"type\":\"BUS\",\"agencyId\":\"12\",\"routeId\":\"05R\",\"routeShortName\":\"5\",\"tripId\":\"0002534672014091020150609\"}},{\"from\":\"Trento FS\",\"to\":\"Rovereto FS\",\"transport\":{\"type\":\"TRAIN\",\"agencyId\":\"5\",\"routeId\":\"BV_R1_G\",\"routeShortName\":\"RG\",\"tripId\":\"R20933\"}},{\"from\":\"Povo Piazza Manci\",\"to\":\"Verona Einaudi\",\"transport\":{\"type\":\"BUS\",\"agencyId\":\"12\",\"routeId\":\"13R\",\"routeShortName\":\"13\",\"tripId\":\"0002551112014091020150609\"}},{\"from\":\"Trento-S.Bartolameo\",\"to\":\"Rovereto-Via Manzoni\",\"transport\":{\"type\":\"BUS\",\"agencyId\":\"17\",\"routeId\":\"110_17_0\",\"routeShortName\":\"301\",\"tripId\":\"0001816952014091020150626\"}},{\"from\":\"Povo Fac. Scienze\",\"to\":\"Piazza Dante Dogana\",\"transport\":{\"type\":\"BUS\",\"agencyId\":\"12\",\"routeId\":\"05R\",\"routeShortName\":\"5\",\"tripId\":\"0002534572014091020150609\"}},{\"from\":\"Via alla Cascata\",\"to\":\"Via Europa\",\"transport\":{\"type\":\"BICYCLE\",\"agencyId\":\"null\",\"routeId\":\"null\",\"routeShortName\":\"null\",\"tripId\":\"null\"}}],\"monitorLegs\":{\"12_05R\":true,\"12_13R\":true,\"16_01R_Rov\":true,\"16_AA_Rov\":true,\"17_110_17_0\":true,\"5_BV_R1_G\":true}}", RecurrentJourney.class); 
	}

	/**
	 * @param i
	 * @return
	 * @throws Exception 
	 */
	public static AlertDelay createTrainDelayForRecurrent(int i) throws Exception {
		AlertDelay delay = new AlertDelay();
		delay.setCreatorId("1");
		delay.setCreatorType(CreatorType.SERVICE);
		delay.setDelay(i*60*1000);
		delay.setType(AlertType.DELAY);
		
		Transport t = new Transport();
		t.setAgencyId("5");
		t.setRouteId("BV_R1_G");
		t.setRouteShortName("");
		t.setTripId("R20931");
		t.setType(TType.TRAIN);
		delay.setTransport(t);
		delay.setFrom(new SimpleDateFormat("ddMMyyyy").parse("23122014").getTime());

		Position p = new Position();
		p.setName("Trento FS");
		delay.setPosition(p);
		
		delay.setId(delay.getTransport().getTripId() + "_" + CreatorType.SERVICE + "_" + delay.getFrom() + "_" + delay.getTo());

		return delay;
	}	
	

	/**
	 * @return
	 */
	public static String idTrainDelayForRecurrent() {
		return "AD_5_BV_R1_G_R20931";
	}

	public static AlertParking createParking(int n) throws Exception {
		AlertParking parking = new AlertParking();
		
		parking.setCreatorId("1");
		parking.setCreatorType(CreatorType.SERVICE);
		parking.setPlacesAvailable(n);
		parking.setType(AlertType.PARKING);
		
		StopId stop = new StopId("COMUNE_DI_ROVERETO", "Rovereto Centro");
		parking.setPlace(stop);
		
		parking.setFrom(System.currentTimeMillis());
		parking.setTo(System.currentTimeMillis() + 1000 * 60 * 5);		

		parking.setId(parking.getPlace().getId() + "_" + CreatorType.SERVICE + "_" + parking.getFrom() + "_" + parking.getTo());
		
		return parking;
	}	
	

	public static AlertParking createBikeSharingFrom(int n) throws Exception {
		AlertParking parking = new AlertParking();
		
		parking.setCreatorId("1");
		parking.setCreatorType(CreatorType.SERVICE);
		parking.setNoOfvehicles(n);
		parking.setType(AlertType.PARKING);
		
		StopId stop = new StopId("BIKE_SHARING_ROVERETO", "Parco Venezia");
		parking.setPlace(stop);
		
		parking.setFrom(System.currentTimeMillis());
		parking.setTo(System.currentTimeMillis() + 1000 * 60 * 5);		

		parking.setId(parking.getPlace().getId() + "_" + CreatorType.SERVICE + "_" + parking.getFrom() + "_" + parking.getTo());
		
		return parking;
	}		

	public static AlertParking createBikeSharingTo(int n) throws Exception {
		AlertParking parking = new AlertParking();
		
		parking.setCreatorId("1");
		parking.setCreatorType(CreatorType.SERVICE);
		parking.setPlacesAvailable(n);
		parking.setType(AlertType.PARKING);
		
		StopId stop = new StopId("BIKE_SHARING_ROVERETO", "Biki Piazzale Follone - Rovereto");
		parking.setPlace(stop);
		
		parking.setFrom(System.currentTimeMillis());
		parking.setTo(System.currentTimeMillis() + 1000 * 60 * 5);		

		parking.setId(parking.getPlace().getId() + "_" + CreatorType.SERVICE + "_" + parking.getFrom() + "_" + parking.getTo());
		
		return parking;
	}		

	/**
	 * @param i
	 * @return
	 * @throws Exception 
	 */
	public static AlertDelay createAlertDelay(int i) throws Exception {
		AlertDelay delay = new AlertDelay();
		delay.setCreatorId("1");
		delay.setCreatorType(CreatorType.USER);
		delay.setDelay(i*60*1000);
		delay.setType(AlertType.DELAY);
		
		Transport t = new Transport();
		t.setAgencyId("5");
		t.setRouteId("BV_R1_G");
		t.setRouteShortName("");
		t.setTripId("R10965");
		t.setType(TType.TRAIN);
		delay.setTransport(t);
		delay.setFrom(new SimpleDateFormat("ddMMyyyy").parse("22122014").getTime());
		delay.setTo(new SimpleDateFormat("ddMMyyyy").parse("23122014").getTime());

		Position p = new Position();
		p.setName("Trento FS");
		delay.setPosition(p);

		return delay;
	}
	
	

}
