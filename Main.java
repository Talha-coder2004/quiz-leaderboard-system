import java.io.*;
import java.net.*;
import java.util.*;
import org.json.*;

public class Main {

    public static void main(String[] args) throws Exception {

        String regNo = "RA2311004040036";

        Set<String> uniqueSet = new HashSet<>();
        Map<String, Integer> scores = new HashMap<>();

        for (int i = 0; i < 10; i++) {

            String urlStr = "https://devapigw.vidalhealthtpa.com/srm-quiz-task/quiz/messages?regNo="
                    + regNo + "&poll=" + i;

            URL url = new URL(urlStr);

            boolean success = false;
            String responseData = "";

            int attempts = 0;

            while (!success && attempts < 5) {
                try {
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");

                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));

                    String line;
                    StringBuilder response = new StringBuilder();

                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    responseData = response.toString();
                    success = true;

                } catch (IOException e) {
                    attempts++;
                    System.out.println("Retrying poll " + i + " attempt " + attempts);
                    Thread.sleep(3000);
                }
            }

            if (!success) {
                System.out.println("Skipping poll " + i);
                continue;
            }


            JSONObject obj = new JSONObject(responseData);
            JSONArray events = obj.getJSONArray("events");

            for (int j = 0; j < events.length(); j++) {

                JSONObject event = events.getJSONObject(j);

                String roundId = event.getString("roundId");
                String participant = event.getString("participant");
                int score = event.getInt("score");

                String key = roundId + "_" + participant;

                if (!uniqueSet.contains(key)) {
                    uniqueSet.add(key);

                    scores.put(participant,
                            scores.getOrDefault(participant, 0) + score);
                }
            }

            Thread.sleep(7000);
        }

        List<Map.Entry<String, Integer>> list = new ArrayList<>(scores.entrySet());
        list.sort((a, b) -> b.getValue() - a.getValue());

        int total = 0;
        JSONArray leaderboardArray = new JSONArray();

        System.out.println("Leaderboard:");

        for (Map.Entry<String, Integer> entry : list) {

            System.out.println(entry.getKey() + " : " + entry.getValue());
            total += entry.getValue();

            JSONObject player = new JSONObject();
            player.put("participant", entry.getKey());
            player.put("totalScore", entry.getValue());

            leaderboardArray.put(player);
        }

        System.out.println("Total Score: " + total);

        try {
            String postUrl = "https://devapigw.vidalhealthtpa.com/srm-quiz-task/quiz/submit";
            URL submitUrl = new URL(postUrl);

            HttpURLConnection conn = (HttpURLConnection) submitUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JSONObject finalData = new JSONObject();
            finalData.put("regNo", regNo);
            finalData.put("leaderboard", leaderboardArray);

            OutputStream os = conn.getOutputStream();
            os.write(finalData.toString().getBytes());
            os.flush();
            os.close();

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));

            String line;
            StringBuilder response = new StringBuilder();

            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            System.out.println("\nSubmission Response:");
            System.out.println(response.toString());

        } catch (IOException e) {
            System.out.println("\nSubmission failed (server issue)");
        }
    }
}