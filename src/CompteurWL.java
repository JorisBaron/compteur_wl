
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class CompteurWL {

	public static final Charset FILE_CHARSET = Charset.forName("UTF-8");
	public static final Path WL_PATH = Paths.get("files/win_lose.txt");
	private static final Path DATA_PATH = Paths.get("files/data");

	private static final String API_BASE_URL = "https://mncccompany.cloud.tyk.io/riot-api/";
	private static final String MATCHLIST_URL = API_BASE_URL + "matchlists/";
	private static final String MATCH_URL = API_BASE_URL + "match/";

	private static final String AUTH_KEY = new Scanner(
			CompteurWL.class.getClassLoader().getResourceAsStream("auth-key")).nextLine();
	private static final long GOB_ID = 21660361;

	// remake Id : 3728299547

	private long lastRequestTimestamp;
	private JFrameConsole console;

	public JFrameConsole getConsole() {
		return this.console;
	}

	public CompteurWL() {
		this.lastRequestTimestamp = System.currentTimeMillis();
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					CompteurWL.this.console = new JFrameConsole();
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void print(String text) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				CompteurWL.this.console.consolePrint(text);
			}
		});
	}

	public void println(String text) {
		this.print(text + System.getProperty("line.separator"));
	}

	public void println() {
		this.print(System.getProperty("line.separator"));
	}

	/**
	 * test si les fichier sont valides, es reset au besoin
	 */
	public void testFichier() {

		File folder = new File("files/");
		if (!folder.exists()) {
			folder.mkdirs();
			this.resetDataFile();
		} else {
			// gestion des fichier data
			if (!DATA_PATH.toFile().exists()) {
				this.println("Fichier data inexistant");
				this.resetDataFile();
				this.println("Fichier data créé");
			} else {

				JSONObject dataJson = CompteurWL.readJson();

				// gestion WL + time
				try {
					JSONObject compteur = dataJson.getJSONObject("compteur");
					compteur.getInt("win");
					compteur.getInt("loss");

					long timestamp = dataJson.getLong("lastTimestamp");
					if (timestamp < this.getMonthTimestamp()) {
						// debut de mois
						this.println("Début de mois, remise à 0");
						this.resetDataFile();
					}
				} catch (JSONException e) {
					this.println("Data corrompu");
					this.resetData();
					this.println("Data reset");
				}

				// gestion offsest
				try {
					JSONObject offsets = dataJson.getJSONObject("offsets");
					offsets.getInt("win");
					offsets.getInt("loss");
				} catch (JSONException e) {
					this.println("Offsets corrompu");
					this.resetOffsets();
					this.println("Offsets reset");
				}

			}
		}
	}

	public static void writeJson(JSONObject json) {
		try (BufferedWriter writer = Files.newBufferedWriter(DATA_PATH, FILE_CHARSET)) {
			json.write(writer);
		} catch (IOException | JSONException ex) {
			ex.printStackTrace();
		}
	}

	public static JSONObject readJson() {
		try (BufferedReader reader = Files.newBufferedReader(DATA_PATH, FILE_CHARSET)) {
			return new JSONObject(new JSONTokener(reader));
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * reset les fichier de WL et de timestamp
	 */
	public void resetDataFile() {

		CompteurWL.writeJson(new JSONObject().put("lastTimestamp", this.getMonthTimestamp())
				.put("compteur", new JSONObject().put("win", 0).put("loss", 0))
				.put("offsets", new JSONObject().put("win", 0).put("loss", 0)));

	}

	public void resetData() {
		try {
			CompteurWL.writeJson(CompteurWL.readJson().put("lastTimestamp", this.getMonthTimestamp()).put("compteur",
					new JSONObject().put("win", 0).put("loss", 0)));
		} catch (JSONException e) {
			this.resetDataFile();
		}
	}

	/**
	 * reset le fichier d'offset
	 */
	public void resetOffsets() {
		try {
			CompteurWL.writeJson(CompteurWL.readJson().put("offsets", new JSONObject().put("win", 0).put("loss", 0)));
		} catch (JSONException e) {
			this.resetDataFile();
		}
	}

	/**
	 * donne le timestamp du debut de mois (1j 00h 00min 00s 000ms)
	 * 
	 * @return timestamp du debut du mois
	 */
	public long getMonthTimestamp() {
		GregorianCalendar cal = new GregorianCalendar();
		// cal.setTimeZone(TimeZone.getTimeZone("GMT"));
		cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 1, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTimeInMillis();
	}

	/**
	 * retourn le dernier timestamp de game connu
	 * 
	 * @return timestamp de la dernière game analysée
	 */
	public long getLastTimestamp() {
		long lastTime;
		try {
			lastTime = CompteurWL.readJson().getLong("lastTimestamp");
		} catch (JSONException e) {
			e.printStackTrace();
			this.resetDataFile();
			lastTime = getMonthTimestamp();
		}

		return lastTime;
	}

	/**
	 * Update le dernier timestamp
	 * 
	 * @param lastTimestamp
	 */
	public void updateLastTimestamp(long lastTimestamp) {
		try {
			CompteurWL.readJson().put("lastTimestamp", lastTimestamp);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * recupere un JSON depuis une url de l'API
	 * 
	 * @param strUrl
	 *            url de la requete ver l'api
	 * @return le JSONObject correspondant a la requete
	 */
	public JSONObject getJsonFromUrl(String strUrl) {
		JSONObject json = new JSONObject();
		int nbTry = 0;
		try {
			long delaySinceLastRequest = System.currentTimeMillis() - this.lastRequestTimestamp;
			if (delaySinceLastRequest < 1200L) {
				TimeUnit.MILLISECONDS.sleep(1250L - delaySinceLastRequest);
			}

			while (nbTry < 10 && json.isEmpty()) {
				try {
					HttpURLConnection httpConnect = (HttpURLConnection) new URL(strUrl).openConnection();
					httpConnect.setRequestMethod("GET");
					httpConnect.setRequestProperty("Authorization", AUTH_KEY);
					httpConnect.connect();
					this.lastRequestTimestamp = System.currentTimeMillis();

					int code = httpConnect.getResponseCode();
					if (code == 404) {
						json.put("matches", new JSONArray());
						json.put("beginIndex", 0);
						json.put("endIndex", 0);
						json.put("totalGames", 0);
					} else if (code == 429) {
						if (nbTry % 2 == 0) {
							this.println("Limite de requêtes depassée. Mise en attente...");
						}
						TimeUnit.SECONDS.sleep(5);
						nbTry++;
					} else if (code == 200) {
						Scanner scan = new Scanner(httpConnect.getInputStream());
						String str = new String();
						while (scan.hasNext())
							str += scan.nextLine();
						scan.close();
						json = new JSONObject(str);
					} else {
						TimeUnit.SECONDS.sleep(1);
						nbTry++;
						if (nbTry == 10) {
							this.println(httpConnect.getResponseMessage());
						}
					}
				} catch (IOException e) {
					nbTry++;
					if (nbTry == 10) {
						e.printStackTrace();
					}
				}
			}

		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (json.isEmpty()) {
			json.put("matches", new JSONArray());
			json.put("beginIndex", 0);
			json.put("endIndex", 0);
			json.put("totalGames", 0);
		}
		return json;
	}

	/**
	 * recupere la liste des ID de game des match entre le timestamp actuel et le
	 * timestamp en parametre
	 * 
	 * @param timestamp
	 *            jusqou'ou il faut remonter (non inclus)
	 * @return liste des gameId
	 */
	public ArrayList<String[]> getMatchHistory(long timestamp) {
		timestamp += 1;

		ArrayList<String[]> listId = new ArrayList<String[]>();

		long currentTimestamp = System.currentTimeMillis();
		String strUrl = MATCHLIST_URL + GOB_ID + "?queue=420";

		if (currentTimestamp - timestamp > 604800000) {// delai de plus d'1 semaine
			// on fait par tranche de 10 matchs
			int index = 0;
			long matchTimestamp = currentTimestamp;

			JSONObject matchesJson = getJsonFromUrl(strUrl + "&beginIndex=" + index + "&endIndex=" + (index + 10));
			JSONArray matchesArray = matchesJson.getJSONArray("matches");

			this.console.newProgressBar(0, 100000);
			int i = 0;
			while (matchTimestamp > timestamp && !matchesArray.isEmpty()) {
				if (i < matchesArray.length()) {
					JSONObject match = matchesArray.getJSONObject(i);
					matchTimestamp = match.getLong("timestamp");
					if (matchTimestamp > timestamp) {
						String[] gameInfo = { String.valueOf(match.getLong("gameId")), String.valueOf(matchTimestamp) };
						listId.add(gameInfo);
					}
					this.console.setProgressBar(Math.toIntExact(
							100000 - ((matchTimestamp - timestamp) * 100000) / (currentTimestamp - timestamp)));

					i++;
				} else {
					index += 10;
					matchesArray = getJsonFromUrl(strUrl + "&beginIndex=" + index + "&endIndex=" + (index + 10))
							.getJSONArray("matches");
					i = 0;
				}
			}
			this.console.removeProgressBar();

		} else {
			// on fait par timestamp
			JSONArray matchesArray = getJsonFromUrl(strUrl + "&beginTime=" + timestamp + "&endTime=" + currentTimestamp)
					.getJSONArray("matches");
			for (int i = 0; i < matchesArray.length(); i++) {
				JSONObject match = matchesArray.getJSONObject(i);
				String[] gameInfo = { String.valueOf(match.getLong("gameId")),
						String.valueOf(match.getLong("timestamp")) };
				listId.add(gameInfo);
			}
		}

		return listId;

	}

	/**
	 * retourne la liste des GameId depuis le dernier timestamp connu
	 * 
	 * @return
	 */
	public ArrayList<String[]> getMatchHistory() {
		return this.getMatchHistory(this.getLastTimestamp());
	}

	/**
	 * retourne le resultat du match passé en parametre
	 * 
	 * @param gameId
	 *            id du match
	 * @return 1 si win, -1 si loss, 0 si remake ou erreur
	 */
	public int getGameResult(String gameId) {
		JSONObject matchJson = this.getJsonFromUrl(MATCH_URL + gameId);
		if (matchJson.getLong("gameDuration") < 240) {
			return 0;
		} else {
			int gameResult = 0;

			JSONArray listParticipantsIdentity = matchJson.getJSONArray("participantIdentities");
			int i = 0;
			int participantId = -1;
			while (participantId < 0 && i < listParticipantsIdentity.length()) {
				JSONObject participant = listParticipantsIdentity.getJSONObject(i);
				if (participant.getJSONObject("player").getLong("accountId") == GOB_ID) {
					participantId = participant.getInt("participantId");
				}
				i++;
			}

			if (participantId >= 0) {
				JSONArray listParticipants = matchJson.getJSONArray("participants");
				JSONObject participant = listParticipants.getJSONObject(participantId - 1);
				if (participant.getInt("participantId") == participantId) {
					if (participant.getJSONObject("stats").getBoolean("win")) {
						gameResult = 1;
					} else {
						gameResult = -1;
					}
				} else {
					// des fois que ce soit décalé ... sait on jamais !
					i = 0;

					while (gameResult == 0 && i < listParticipants.length()) {
						participant = listParticipants.getJSONObject(i);
						if (participant.getInt("participantId") == participantId) {
							if (participant.getJSONObject("stats").getBoolean("win")) {
								gameResult = 1;
							} else {
								gameResult = -1;
							}
						}
						i++;
					}
				}
			}
			return gameResult;
		}
	}

	/**
	 * Donne le nombre de win et de loss SANS offsets
	 * 
	 * @return tableau contenant les win a l'index 0 et les loss a l'index 1
	 */
	public int[] getActualWL() {
		int[] wl = new int[2];
		try {
			JSONObject compteur = CompteurWL.readJson().getJSONObject("compteur");
			wl[0] = compteur.getInt("win");
			wl[1] = compteur.getInt("loss");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return wl;
	}

	/**
	 * retourne les offsets de score
	 * 
	 * @return tableau contenant les offsets : win a l'index 0 et les loss a l'index 1
	 */
	public static int[] getOffsets() {
		int[] offset = new int[2];
		try {
			JSONObject offsets = CompteurWL.readJson().getJSONObject("offsets");
			offset[0] = offsets.getInt("win");
			offset[1] = offsets.getInt("loss");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return offset;
	}

	public static void setOffset(int[] offset) {
		try {
			JSONObject dataJson = CompteurWL.readJson();
			dataJson.getJSONObject("offsets").put("win", offset[0]);
			dataJson.getJSONObject("offsets").put("loss", offset[1]);
			CompteurWL.writeJson(dataJson);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Donne le nombre de win et de loss AVEC offsets
	 * 
	 * @return tableau contenant les win a l'index 0 et les loss a l'index 1
	 */
	public int[] getWL() {
		int[] wl = new int[2];
		try {
			JSONObject dataJson = CompteurWL.readJson();
			wl[0] = dataJson.getJSONObject("compteur").getInt("win") - dataJson.getJSONObject("offsets").getInt("win");
			wl[1] = dataJson.getJSONObject("compteur").getInt("loss")
					- dataJson.getJSONObject("offsets").getInt("loss");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return wl;
	}

	/**
	 * mets a jour le fichier de WL affiché avec les valeur de WL et d'offsets
	 */
	public void refreshWL(int[] wl) {
		try (BufferedWriter wlWriter = Files.newBufferedWriter(WL_PATH, FILE_CHARSET)) {
			wlWriter.write(wl[0] + "/" + wl[1]);
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
	}

	public int[] refreshWL() {
		int[] wl = this.getWL();
		this.refreshWL(getWL());
		return wl;
	}

	/**
	 * ajoute une win ou une loss en fonction du parametre
	 * 
	 * @param gameResult
	 *            la resultat d'un match
	 */
	public void addWL(int gameResult, long gameTimestamp) {

		try {
			JSONObject dataJson = CompteurWL.readJson();
			if (gameResult == 1) {
				dataJson.getJSONObject("compteur").increment("win");
			} else if (gameResult == -1) {
				dataJson.getJSONObject("compteur").increment("loss");
			}
			dataJson.put("lastTimestamp", gameTimestamp);
			CompteurWL.writeJson(dataJson);
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}

	public void exe() {
		this.testFichier();
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					CompteurWL.setOffset(CompteurWL.this.console.askOffsets(CompteurWL.getOffsets()));
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// this.console.removeKeyListener(this.console.getKeyListeners()[0]);
		while (true) {

			this.println("Actualisation des matchs...");
			ArrayList<String[]> listMatches = this.getMatchHistory();

			int nbMatchs = listMatches.size();
			this.println(nbMatchs + " nouveaux matchs trouvés");

			if (nbMatchs > 0) {
				this.println("Analyse des matchs...");
				this.console.newProgressBar(0, nbMatchs);

				for (int i = 1; i <= nbMatchs; i++) {

					String[] match = listMatches.get(nbMatchs - i);
					int result = this.getGameResult(match[0]);

					this.console.setProgressBar(i);

					this.addWL(result, Long.parseLong(match[1]));

					int[] wl = this.refreshWL();

					// affichage
					this.print("match " + i + " : ");
					if (result > 0)
						this.print("win");
					else if (result == 0)
						this.print("remake");
					else
						this.print("loss");
					this.println(" (" + wl[0] + "/" + wl[1] + ")");
				}

				this.console.removeProgressBar();
				int[] newWL = this.getWL();
				this.println("Analyse terminée");
				this.println("Nouveau score : " + newWL[0] + "/" + newWL[1]);
			} else {
				int[] wl = this.refreshWL();
				this.println("Score actuel : " + wl[0] + "/" + wl[1]);
			}

			this.testFichier();
			this.println("Mise en attente...");
			this.println("");

			try {
				TimeUnit.MINUTES.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
