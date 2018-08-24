import java.io.File;

public class RunCompteur {
	//private static final String AUTH_Key = new Scanner(RunCompteur.class.getResourceAsStream("/auth-key")).nextLine();
	
	public static void main(String[] args)  {
		
		System.out.println(new File("files/").exists());
		CompteurWL compt = new CompteurWL();
		compt.exe();
	}

	
	
}
