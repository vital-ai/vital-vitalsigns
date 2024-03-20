
from vital_ai_vitalsigns.model.properties.BooleanProperty import BooleanProperty
from vital_ai_vitalsigns.model.properties.DateTimeProperty import DateTimeProperty
from vital_ai_vitalsigns.model.properties.DoubleProperty import DoubleProperty
from vital_ai_vitalsigns.model.properties.FloatProperty import FloatProperty
from vital_ai_vitalsigns.model.properties.GeoLocationProperty import GeoLocationProperty
from vital_ai_vitalsigns.model.properties.IntegerProperty import IntegerProperty
from vital_ai_vitalsigns.model.properties.LongProperty import LongProperty
from vital_ai_vitalsigns.model.properties.OtherProperty import OtherProperty
from vital_ai_vitalsigns.model.properties.StringProperty import StringProperty
from vital_ai_vitalsigns.model.properties.TruthProperty import TruthProperty
from vital_ai_vitalsigns.model.properties.URIProperty import URIProperty
from vital_ai_domain.model.VitalDataScript import VitalDataScript


class Job(VitalDataScript):
    _allowed_properties = [
        {'uri': 'http://vital.ai/ontology/vital#hasCronExpression', 'prop_class': StringProperty}, 
        {'uri': 'http://vital.ai/ontology/vital#hasInterval', 'prop_class': IntegerProperty}, 
        {'uri': 'http://vital.ai/ontology/vital#hasIntervalTimeUnit', 'prop_class': StringProperty}, 
        {'uri': 'http://vital.ai/ontology/vital#hasLastExecutionTime', 'prop_class': DateTimeProperty}, 
        {'uri': 'http://vital.ai/ontology/vital#hasNextExecutionTime', 'prop_class': DateTimeProperty}, 
        {'uri': 'http://vital.ai/ontology/vital#isCallable', 'prop_class': BooleanProperty}, 
        {'uri': 'http://vital.ai/ontology/vital#isPaused', 'prop_class': BooleanProperty}, 
    ]

    @classmethod
    def get_allowed_properties(cls):
        return super().get_allowed_properties() + Job._allowed_properties

