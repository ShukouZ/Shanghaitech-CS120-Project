import javax.crypto.spec.PSource;
import java.io.IOException;
import java.sql.SQLOutput;
import java.util.*;

public class CommandFix {
    // This class is meant to fix the potential incorrect input
    // command and return a decent response
    // The method of calculating the distance comes from
    // https://zhuanlan.zhihu.com/p/91645988
    static String command_fix(String command){
        command = command.toUpperCase();
        List<String> commandList = Arrays.asList("USER", "PASS", "PWD", "CWD", "PASV", "LIST", "RETR");
        List<Integer> disList = Levenshtein(command, commandList);
        if(disList == null){
            return "";
        }
        int minIdx = disList.indexOf(Collections.min(disList));
        int minChangeNum = disList.get(minIdx);
        if(minChangeNum >= 3){
            System.out.println("INVALID COMMAND!");
            System.out.println("Please consider using one of the following commands?");
            commandList.forEach(p -> System.out.print(" " + p));
            System.out.println();
            return "";
        }
        List<String> minDisCommands = calRepetition(minChangeNum, disList, commandList);
        if(minDisCommands.size() == 1){
            return minDisCommands.get(0);
        }else{
            System.out.println("You may want to input one of the following commands:");
            minDisCommands.forEach(p -> System.out.print(" " + p));
            System.out.println();
            return "";
        }
    }

    public static List<Integer> Levenshtein(String a, List<String> list) {
        if (a == null) {
            return null;
        }
        List<Integer> disList = new ArrayList<>();
        for(String b: list){
            disList.add(editDis(a, b));
        }

        return disList;
    }

    private static int editDis(String a, String b) {
        int aLen = a.length();
        int bLen = b.length();

        if (aLen == 0) return bLen;
        if (bLen == 0) return aLen;

        int[][] v = new int[aLen + 1][bLen + 1];
        for (int i = 0; i <= aLen; ++i) {
            for (int j = 0; j <= bLen; ++j) {
                if (i == 0) {
                    v[i][j] = j;
                } else if (j == 0) {
                    v[i][j] = i;
                } else if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    v[i][j] = v[i - 1][j - 1];
                } else {
                    v[i][j] = 1 + Math.min(v[i - 1][j - 1], Math.min(v[i][j - 1], v[i - 1][j]));
                }
            }
        }
        return v[aLen][bLen];
    }

    private static List<String> calRepetition(Integer a, List<Integer> disList, List<String> commandList){
        int listLen = disList.size();
        List<String> returnCommand = new ArrayList<>();
        for(int i=0; i<listLen; i++){
            if(Objects.equals(a, disList.get(i))){
                returnCommand.add(commandList.get(i));
            }
        }
        return returnCommand;
    }

    public static void main(final String[] args) throws IOException {
        Scanner in = new Scanner(System.in);
        String a = in.nextLine().strip();
        System.out.println(command_fix(a));
    }
}
