package monoton.control.events.client;

public class Event {

    public boolean isCancel;

    public boolean isCancel() {
        return isCancel;
    }

    public void open() {
        isCancel = false;
    }

    public void setCancel(boolean cancel) {
        this.isCancel = cancel;
    }

    public void cancel() {
        isCancel = true;
    }

}