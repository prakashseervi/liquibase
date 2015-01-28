package liquibase.actionlogic.core;

import liquibase.Scope;
import liquibase.action.Action;
import liquibase.action.core.AddDefaultValueAction;
import liquibase.action.core.RedefineColumnAction;
import liquibase.actionlogic.AbstractActionLogic;
import liquibase.actionlogic.ActionResult;
import liquibase.actionlogic.RewriteResult;
import liquibase.database.Database;
import liquibase.datatype.DataTypeFactory;
import liquibase.datatype.LiquibaseDataType;
import liquibase.datatype.core.BooleanType;
import liquibase.datatype.core.CharType;
import liquibase.exception.ActionPerformException;
import liquibase.exception.ValidationErrors;
import liquibase.statement.SequenceNextValueFunction;

public class AddDefaultValueLogic extends AbstractActionLogic {

    @Override
    protected Class<? extends Action> getSupportedAction() {
        return AddDefaultValueAction.class;
    }

    @Override
    public ValidationErrors validate(Action action, Scope scope) {
        Object defaultValue = action.get(AddDefaultValueAction.Attr.defaultValue, Object.class);

        Database database = scope.get(Scope.Attr.database, Database.class);

        ValidationErrors validationErrors = super.validate(action, scope);
        validationErrors.checkForRequiredField(AddDefaultValueAction.Attr.defaultValue, action);
        validationErrors.checkForRequiredField(AddDefaultValueAction.Attr.columnName, action);
        validationErrors.checkForRequiredField(AddDefaultValueAction.Attr.tableName, action);
        if (!database.supportsSequences() && defaultValue instanceof SequenceNextValueFunction) {
            validationErrors.addError("Database " + database.getShortName() + " does not support sequences");
        }

        String columnDataType = action.get(AddDefaultValueAction.Attr.columnDataType, String.class);
        if (columnDataType != null) {
            LiquibaseDataType dataType = DataTypeFactory.getInstance().fromDescription(columnDataType, database);
            boolean typeMismatch = false;
            if (dataType instanceof BooleanType) {
                if (!(defaultValue instanceof Boolean)) {
                    typeMismatch = true;
                }
            } else if (dataType instanceof CharType) {
                if (!(defaultValue instanceof String)) {
                    typeMismatch = true;
                }
            }

            if (typeMismatch) {
                validationErrors.addError("Default value of " + defaultValue + " does not match defined type of " + columnDataType);
            }
        }

        return validationErrors;
    }

    @Override
    public ActionResult execute(Action action, Scope scope) throws ActionPerformException {
        return new RewriteResult(new RedefineColumnAction(
                action.get(AddDefaultValueAction.Attr.catalogName, String.class),
                action.get(AddDefaultValueAction.Attr.schemaName, String.class),
                action.get(AddDefaultValueAction.Attr.tableName, String.class),
                action.get(AddDefaultValueAction.Attr.columnName, String.class),
                getDefaultValueClause(action, scope)
        ));
    }

    protected String getDefaultValueClause(Action action, Scope scope) {
        Database database = scope.get(Scope.Attr.database, Database.class);
        Object defaultValue = action.get(AddDefaultValueAction.Attr.defaultValue, Object.class);

        return "DEFAULT " + DataTypeFactory.getInstance().fromObject(defaultValue, database).objectToSql(defaultValue, database);
    }
}
