from sqlalchemy import Engine, create_engine, PrimaryKeyConstraint
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column


def init_db() -> Engine:
    return create_engine("sqlite:///dictionary.db", echo=True)


def init_tables(db: Engine):
    Base.metadata.drop_all(db)
    Base.metadata.tables['Foreign'].create(db)
    Base.metadata.tables['Native'].create(db)
    Base.metadata.tables['Meaning'].create(db)


class Base(DeclarativeBase):
    pass


class Foreign(Base):
    __tablename__ = 'Foreign'
    word: Mapped[str] = mapped_column(primary_key=True)
    attempts: Mapped[str] = mapped_column(nullable=False)
    modified: Mapped[int] = mapped_column(nullable=True)


class Native(Base):
    __tablename__ = 'Native'
    word: Mapped[str] = mapped_column(primary_key=True)
    attempts: Mapped[str] = mapped_column(nullable=False)
    modified: Mapped[int] = mapped_column(nullable=True)


class Meaning(Base):
    __tablename__ = 'Meaning'
    foreign: Mapped[str] = mapped_column(primary_key=True, nullable=False)
    native: Mapped[str] = mapped_column(primary_key=True, nullable=False)
    part: Mapped[str] = mapped_column(nullable=False)

    __table_args__ = (
        PrimaryKeyConstraint(foreign, native),
    )
