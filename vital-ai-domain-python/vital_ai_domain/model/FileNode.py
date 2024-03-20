
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
from vital_ai_vitalsigns.model.VITAL_Node import VITAL_Node


class FileNode(VITAL_Node):
    _allowed_properties = [
        {'uri': 'http://vital.ai/ontology/vital#hasAccountURI', 'prop_class': URIProperty}, 
        {'uri': 'http://vital.ai/ontology/vital#hasExpirationDate', 'prop_class': DateTimeProperty}, 
        {'uri': 'http://vital.ai/ontology/vital#hasFileLength', 'prop_class': LongProperty}, 
        {'uri': 'http://vital.ai/ontology/vital#hasFileName', 'prop_class': StringProperty}, 
        {'uri': 'http://vital.ai/ontology/vital#hasFileScope', 'prop_class': StringProperty}, 
        {'uri': 'http://vital.ai/ontology/vital#hasFileType', 'prop_class': StringProperty}, 
        {'uri': 'http://vital.ai/ontology/vital#hasFileURL', 'prop_class': StringProperty}, 
        {'uri': 'http://vital.ai/ontology/vital#hasProfileURI', 'prop_class': URIProperty}, 
    ]

    @classmethod
    def get_allowed_properties(cls):
        return super().get_allowed_properties() + FileNode._allowed_properties
