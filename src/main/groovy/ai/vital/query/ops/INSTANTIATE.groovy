package ai.vital.query.ops


import ai.vital.vitalsigns.VitalSigns;
import ai.vital.vitalsigns.model.VitalApp

class INSTANTIATE {

    private String app
    
    List<InstanceOp> instances = []
    
    private VitalApp _app = null
    
    
    public void setApp(String app) {
        this.app = app
        _app = null
    }
    
    public String getApp() {
        return app
    }
    
    VitalApp getAppObj() {
        
        if(_app != null) return _app
        
        if(app != null) {
            _app = new VitalApp()
            _app.appID = app
        } else {
        
            _app = VitalSigns.get().getCurrentApp()
            if(_app == null) throw new RuntimeException("VitalSigns current app not set");
        }
        
        return _app
        
    }
    
}
