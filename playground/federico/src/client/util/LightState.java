package client.util;

public class LightState {

	public int ID;
	public int State;
	
	public LightState(int _ID, int _State) {
		ID = _ID;
		State = _State;
	}

        public String toString() {
            return "ID = " + Integer.toString(ID) + ", State = " + Integer.toString(State);
        }
}
